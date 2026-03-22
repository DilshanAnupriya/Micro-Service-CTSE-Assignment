# DevOps Implementation Guide — Vehicle Microservice

> **Stack**: Spring Boot 4.0.3 | Java 17 | MySQL 8 | Docker | Docker Hub | GitHub Actions | AWS ECS Fargate

---

## Table of Contents

1. [Version Control — GitHub Setup](#1-version-control--github-setup)
2. [Containerize with Docker](#2-containerize-with-docker)
3. [Push Image to Docker Hub](#3-push-image-to-docker-hub)
4. [CI/CD Pipeline — GitHub Actions](#4-cicd-pipeline--github-actions)
5. [AWS Infrastructure Setup](#5-aws-infrastructure-setup)
6. [Security Configuration](#6-security-configuration)
7. [Deploy to AWS ECS Fargate](#7-deploy-to-aws-ecs-fargate)
8. [Verify the Deployment](#8-verify-the-deployment)
9. [Quick Reference — GitHub Secrets](#9-quick-reference--github-secrets)

---

## 1. Version Control — GitHub Setup

### 1.1 Ensure your repo is public

```bash
# Check current remote
git remote -v

# If you haven't pushed yet:
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/Micro-Service-CTSE-Assignment.git
```

Go to: **GitHub → Your Repo → Settings → General → Danger Zone → Change visibility → Public**

### 1.2 Commit and push the new DevOps files

```bash
git add Dockerfile .dockerignore docker-compose.yml \
        .github/workflows/ci-cd.yml \
        src/main/resources/application.properties \
        src/main/resources/application-prod.properties \
        DEVOPS_GUIDE.md
git commit -m "feat: add DevOps configuration (Docker, CI/CD, cloud deploy)"
git push origin main
```

---

## 2. Containerize with Docker

### 2.1 Install Docker Desktop
Download from: https://www.docker.com/products/docker-desktop  
Verify: `docker --version`

### 2.2 Build the image locally

```bash
# From project root (where Dockerfile lives)
cd "Micro-Service-CTSE-Assignment"
docker build -t car-rental-vehicle-service:latest .
```

The multi-stage build:
- **Stage 1 (builder)**: Uses `eclipse-temurin:17-jdk-alpine`, downloads Maven dependencies (cached), builds the fat JAR.
- **Stage 2 (runtime)**: Uses slim `eclipse-temurin:17-jre-alpine`, copies only the JAR — no build tools — reduces image size by ~60%.

### 2.3 Run locally with Docker Compose

Create a `.env` file in the project root (this file is **never committed**):

```env
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_USER=appuser
MYSQL_PASSWORD=apppassword
EUREKA_ENABLED=false
```

> **Note**: Add `.env` to your `.gitignore` file.

Start the stack:
```bash
docker-compose up -d
```

- MySQL starts on port `3307` (avoids conflict with local MySQL)
- Vehicle service starts on port `8081`
- The app waits for MySQL health check before starting

Test it:
```bash
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP",...}

curl -X POST http://localhost:8081/api/v1/vehicles \
  -H "Content-Type: application/json" \
  -d '{"make":"Toyota","brand":"Corolla","model":"SE","year":2023,"plateNumber":"ABC-123","dailyRate":50.00,"mileage":0,"color":"White","imageUrl":"","description":"New car"}'
```

Stop: `docker-compose down`

---

## 3. Push Image to Docker Hub

### 3.1 Create a Docker Hub account
Sign up at: https://hub.docker.com — choose a free plan.

### 3.2 Create a public repository
Docker Hub → **Create Repository** → Name: `car-rental-vehicle-service` → **Public** → Create.

### 3.3 Tag and push manually (first time)

```bash
docker login -u <YOUR_DOCKERHUB_USERNAME>

docker tag car-rental-vehicle-service:latest \
  <YOUR_DOCKERHUB_USERNAME>/car-rental-vehicle-service:latest

docker push <YOUR_DOCKERHUB_USERNAME>/car-rental-vehicle-service:latest
```

Your image is now public at:  
`https://hub.docker.com/r/<YOUR_DOCKERHUB_USERNAME>/car-rental-vehicle-service`

---

## 4. CI/CD Pipeline — GitHub Actions

The pipeline file is at `.github/workflows/ci-cd.yml` and has **3 jobs**:

| Job | Trigger | What it does |
|-----|---------|--------------|
| `build-test` | Every push + PR | Compiles with Maven, runs unit tests |
| `docker-build-push` | Push to `main` only | Builds Docker image, pushes to Docker Hub |
| `deploy` | Push to `main` only | Updates ECS task definition, triggers rolling deploy |

### 4.1 Add GitHub Secrets

Go to: **GitHub Repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|---|---|
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub password or [Access Token](https://hub.docker.com/settings/security) *(recommended)* |
| `AWS_ACCESS_KEY_ID` | IAM user Access Key *(see Section 5)* |
| `AWS_SECRET_ACCESS_KEY` | IAM user Secret Key |
| `AWS_REGION` | e.g. `us-east-1` |
| `ECS_CLUSTER` | e.g. `vehicle-cluster` |
| `ECS_SERVICE` | e.g. `vehicle-service` |
| `ECS_TASK_DEFINITION` | e.g. `vehicle-task` |

### 4.2 Pipeline in Action

On every `git push origin main`:
1. GitHub Actions spins up an Ubuntu runner
2. Java 17 is set up, Maven builds and tests the app  
3. Docker image is built, tagged with the commit SHA and `latest`, pushed to Docker Hub  
4. ECS task definition is updated with the new image, ECS triggers a rolling deployment  
5. Pipeline waits for the new task to become healthy before marking success

Check the pipeline: **GitHub → Your Repo → Actions** tab.

---

## 5. AWS Infrastructure Setup

> **Estimated cost**: ~$0–5/month using ECS Fargate with 0.25 vCPU / 512MB on the free tier. Use AWS RDS Free Tier for MySQL.

### 5.1 Create an IAM User for CI/CD

1. AWS Console → **IAM → Users → Create user**
2. Name: `github-actions-deploy`
3. Attach policies:
   - `AmazonECS_FullAccess`
   - `AmazonEC2ContainerRegistryReadOnly` (if using ECR)
4. **Security credentials → Create access key** → Application running outside AWS
5. Save the `ACCESS_KEY_ID` and `SECRET_ACCESS_KEY` — add them as GitHub Secrets.

### 5.2 Create an RDS MySQL Instance (Production Database)

1. AWS Console → **RDS → Create database**
2. Engine: **MySQL 8.0**, Template: **Free tier**
3. DB identifier: `vehicle-db`
4. Credentials: set username/password (save these securely!)
5. Connectivity: **Don't allow public access** ✓
6. VPC: use the default VPC
7. Create a new **Security Group**: `rds-sg`
   - Inbound: MySQL (3306) from the ECS security group only
8. Note the **Endpoint** (e.g. `vehicle-db.xxxxxxx.us-east-1.rds.amazonaws.com`)

### 5.3 Create an ECS Cluster

1. AWS Console → **ECS → Clusters → Create cluster**
2. Name: `vehicle-cluster`
3. Infrastructure: **AWS Fargate** (serverless — no EC2 to manage)
4. Create

### 5.4 Create an ECS Task Definition

1. **ECS → Task definitions → Create new task definition**
2. Name: `vehicle-task`
3. Launch type: `AWS Fargate`
4. OS/Arch: `Linux/X86_64`
5. CPU: `0.25 vCPU`, Memory: `512 MB`
6. Container:
   - Name: `vehicle-service` *(must match `CONTAINER_NAME` in the workflow)*
   - Image: `<YOUR_DOCKERHUB_USERNAME>/car-rental-vehicle-service:latest`
   - Port mappings: `8081` TCP
   - Environment variables:
     | Key | Value |
     |---|---|
     | `SPRING_DATASOURCE_URL` | `jdbc:mysql://<RDS-ENDPOINT>:3306/vehicle_service?createDatabaseIfNotExist=true&useSSL=true` |
     | `SPRING_DATASOURCE_USERNAME` | `<RDS username>` |
     | `SPRING_DATASOURCE_PASSWORD` | `<RDS password>` |
     | `EUREKA_CLIENT_ENABLED` | `false` |
     | `SPRING_PROFILES_ACTIVE` | `prod` |
   - Health check: `CMD-SHELL`, `wget -qO- http://localhost:8081/actuator/health || exit 1`
7. Create

### 5.5 Create an Application Load Balancer (ALB)

1. **EC2 → Load Balancers → Create load balancer → Application Load Balancer**
2. Name: `vehicle-alb`
3. Scheme: **Internet-facing**
4. Listeners: HTTP port 80 (add HTTPS 443 if you have a domain/SSL cert)
5. Create a **Security Group** (`alb-sg`): Inbound HTTP 80 + HTTPS 443 from `0.0.0.0/0`
6. Target group: `vehicle-tg`
   - Target type: **IP**
   - Protocol: HTTP, Port: 8081
   - Health check path: `/actuator/health`

### 5.6 Create ECS Service

1. **ECS → Clusters → vehicle-cluster → Services → Create**
2. Launch type: **Fargate**
3. Family: `vehicle-task`
4. Service name: `vehicle-service`
5. Desired tasks: `1`
6. Networking:
   - VPC: same as RDS
   - Subnets: pick 2 public subnets
   - Security group (`ecs-sg`): Inbound TCP 8081 from `alb-sg` only
   - Auto-assign public IP: **Enabled**
7. Load balancing: select `vehicle-alb` → `vehicle-tg`
8. Create

---

## 6. Security Configuration

### 6.1 Network Security (Security Groups)

```
Internet
   │ HTTP/HTTPS
   ▼
[ALB - alb-sg]
   │ TCP 8081 (only from alb-sg)
   ▼
[ECS Tasks - ecs-sg]
   │ MySQL 3306 (only from ecs-sg)
   ▼
[RDS - rds-sg]
```

- **alb-sg**: Inbound 80, 443 from `0.0.0.0/0`
- **ecs-sg**: Inbound 8081 from `alb-sg` only — tasks are NOT directly internet-accessible
- **rds-sg**: Inbound 3306 from `ecs-sg` only — database is NOT publicly accessible

### 6.2 Secrets Management (Do NOT hardcode secrets)

Secrets are passed as **environment variables** to the ECS task. For production, use **AWS Secrets Manager**:

1. AWS Console → **Secrets Manager → Store a new secret**
2. Store DB credentials as key-value pairs
3. In the ECS Task Definition, reference them using `valueFrom` (Secrets Manager ARN) instead of plain `value`

### 6.3 HTTPS (TLS/SSL)

1. Register a domain (Namecheap, Route 53, etc.)
2. **AWS Certificate Manager → Request public certificate** (free)
3. Add an HTTPS **443** listener to the ALB, attach the certificate
4. Add a redirect rule: HTTP → HTTPS

### 6.4 Actuator Security (Production)

`application-prod.properties` already restricts actuator to health + info only. This ensures internal health/metrics data is not publicly accessible.

### 6.5 IAM Least Privilege

The `github-actions-deploy` IAM user has only the minimum permissions needed to update ECS. It cannot access RDS, S3, or other services.

### 6.6 Non-Root Docker User

The `Dockerfile` creates an `appuser` non-root user and runs the JVM process under that user. This follows container security best practices.

---

## 7. Deploy to AWS ECS Fargate

After all GitHub Secrets are configured per Section 4.1:

```bash
git push origin main
```

This automatically triggers the full CI/CD pipeline. Monitor it in **GitHub → Actions**.

### Manual re-deploy (force refresh without code changes)

```bash
aws ecs update-service \
  --cluster vehicle-cluster \
  --service vehicle-service \
  --force-new-deployment \
  --region us-east-1
```

---

## 8. Verify the Deployment

### 8.1 Get the ALB DNS name

AWS Console → **EC2 → Load Balancers → vehicle-alb** → Copy the **DNS name**.

### 8.2 Health check

```bash
curl http://<ALB-DNS>/actuator/health
# Expected: {"status":"UP"}
```

### 8.3 Test CRUD operations

```bash
# Create a vehicle
curl -X POST http://<ALB-DNS>/api/v1/vehicles \
  -H "Content-Type: application/json" \
  -d '{"make":"Toyota","brand":"Corolla","model":"SE","year":2024,"plateNumber":"XYZ-999","dailyRate":65.00,"mileage":100,"color":"Blue","imageUrl":"","description":"Test vehicle"}'

# Get vehicle list
curl "http://<ALB-DNS>/api/v1/vehicles/list?searchText=Toyota&page=0&size=10"
```

### 8.4 Monitor ECS tasks

AWS Console → **ECS → Clusters → vehicle-cluster → Services → vehicle-service → Tasks**  
- Task status should be **RUNNING**
- Health status: **HEALTHY**

---

## 9. Quick Reference — GitHub Secrets

| Secret | Where to find |
|--------|--------------|
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub → Account Settings → Security → New Access Token |
| `AWS_ACCESS_KEY_ID` | IAM → Users → github-actions-deploy → Security credentials |
| `AWS_SECRET_ACCESS_KEY` | Same as above (shown only once on creation) |
| `AWS_REGION` | The AWS region you chose (e.g. `us-east-1`) |
| `ECS_CLUSTER` | ECS → Clusters → cluster name |
| `ECS_SERVICE` | ECS → Clusters → your cluster → Services → service name |
| `ECS_TASK_DEFINITION` | ECS → Task definitions → task family name |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        GitHub                                   │
│  Repository (public)                                            │
│  └── .github/workflows/ci-cd.yml                               │
│        │  push to main                                          │
│        ▼                                                        │
│  ┌───────────┐   ┌─────────────────────┐   ┌────────────────┐  │
│  │ build-test│──▶│ docker-build-push   │──▶│    deploy      │  │
│  │ mvn verify│   │ push to Docker Hub  │   │ ECS Fargate    │  │
│  └───────────┘   └─────────────────────┘   └────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │                        │
                              ▼                        ▼
                      ┌─────────────┐        ┌──────────────────┐
                      │  Docker Hub │        │   AWS ECS        │
                      │  (public    │        │   Fargate        │
                      │   registry) │        │                  │
                      └─────────────┘        │  ┌────────────┐  │
                                             │  │  Task      │  │
Internet ──▶ ALB (HTTPS/HTTP) ──▶ ecs-sg ──▶│  │  vehicle-  │  │
                                             │  │  service   │  │
                                             │  └─────┬──────┘  │
                                             └────────┼─────────┘
                                                      │ MySQL 3306
                                                      ▼
                                              ┌──────────────┐
                                              │  AWS RDS     │
                                              │  MySQL 8.0   │
                                              │  (private)   │
                                              └──────────────┘
```

---

*Generated for CTSE Assignment — Vehicle Microservice DevOps Implementation*
