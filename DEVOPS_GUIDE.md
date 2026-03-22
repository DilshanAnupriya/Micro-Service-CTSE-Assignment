# 🚀 Vehicle Microservice — AWS ECS Deployment Guide
### (Eureka Service Discovery + API Gateway)

---

## Architecture Overview

```
Internet
   │
   ▼
[ALB / API Gateway ECS Service]  ← Routes requests via Eureka discovery
   │
   ▼
[Eureka Server ECS Service]      ← All services register here
   │
   ▼
[Vehicle Service ECS Service]    ← SERVER_PORT=8080 on ECS (random-port only in local dev)
   │
   ▼
[Amazon RDS — MySQL]
```

| Setting | Local Dev | AWS ECS |
|---|---|---|
| `server.port` | `0` (random) | `8080` (forced via ECS task env var) |
| Spring Profile | `default` | `prod` |
| Eureka enabled | `false` | `true` |
| Eureka URL | `localhost:8761` (fallback) | ECS Service Connect internal DNS |

---

## Files Changed (summary)

| File | What Changed |
|---|---|
| `Dockerfile` | `EXPOSE 8081 → 8080`; health check uses `${SERVER_PORT:-8080}` |
| `application-prod.properties` | Fixed app name; added Eureka config for ECS; faster lease timings |
| `.github/workflows/ci-cd.yml` | Injects `SERVER_PORT=8080`, `SPRING_PROFILES_ACTIVE=prod`, Eureka URL, DB creds into ECS task def at deploy time |
| `docker-compose.yml` | **No change** — already overrides `SERVER_PORT=8081` ✅ |
| `application.properties` | **No change** — `server.port=${SERVER_PORT:0}` already works ✅ |

---

## Step 1 — GitHub Repository Secrets

Go to **GitHub → repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|---|---|
| `DOCKER_USERNAME` | Your Docker Hub username |
| `DOCKER_PASSWORD` | Your Docker Hub password / access token |
| `AWS_ACCESS_KEY_ID` | IAM user access key (needs ECS permissions) |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |
| `AWS_REGION` | e.g. `ap-southeast-1` |
| `ECS_CLUSTER` | ECS cluster name, e.g. `microservice-cluster` |
| `ECS_SERVICE` | Vehicle service name, e.g. `vehicle-service` |
| `ECS_TASK_DEFINITION` | Task definition family name, e.g. `vehicle-service-task` |
| `EUREKA_SERVICE_URL` | Internal Eureka URL e.g. `http://eureka-server:8761/eureka/` |
| `SPRING_DATASOURCE_URL` | RDS JDBC URL e.g. `jdbc:mysql://your-rds:3306/vehicle_service` |
| `SPRING_DATASOURCE_USERNAME` | RDS username |
| `SPRING_DATASOURCE_PASSWORD` | RDS password |

> `EUREKA_SERVICE_URL` must use the **ECS Service Connect** internal DNS name — not a public IP.

---

## Step 2 — AWS Infrastructure

### 2a. VPC & Security Groups

All ECS services must be in the **same VPC**. Configure inbound rules:

| Service | Port | Allow From |
|---|---|---|
| Eureka Server | 8761 | Vehicle Service SG, API Gateway SG |
| Vehicle Service | 8080 | API Gateway SG |
| RDS MySQL | 3306 | Vehicle Service SG |

### 2b. Amazon RDS (MySQL)

1. Create **RDS MySQL 8.0** in the same VPC
2. Create database: `vehicle_service`
3. Copy the endpoint — use it as `SPRING_DATASOURCE_URL`

### 2c. ECS Cluster

```bash
aws ecs create-cluster --cluster-name microservice-cluster
```

Or via AWS Console → ECS → Clusters → Create Cluster → **AWS Fargate**.

### 2d. CloudWatch Log Groups

```bash
aws logs create-log-group --log-group-name /ecs/vehicle-service
aws logs create-log-group --log-group-name /ecs/eureka-server
aws logs create-log-group --log-group-name /ecs/api-gateway
```

---

## Step 3 — Deploy Eureka Server to ECS

### 3a. Create `eureka-server-task.json`

```json
{
  "family": "eureka-server-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "eureka-server",
      "image": "<DOCKER_USERNAME>/eureka-server:latest",
      "portMappings": [
        { "containerPort": 8761, "hostPort": 8761, "protocol": "tcp" }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "wget -qO- http://localhost:8761/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/eureka-server",
          "awslogs-region": "ap-southeast-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 3b. Register & Create Eureka Service

```bash
aws ecs register-task-definition --cli-input-json file://eureka-server-task.json

aws ecs create-service \
  --cluster microservice-cluster \
  --service-name eureka-server \
  --task-definition eureka-server-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}"
```

### 3c. Enable Service Connect (internal DNS)

In the AWS Console:
1. Go to **ECS → Clusters → microservice-cluster → Services → eureka-server → Edit**
2. Enable **Service Connect** → namespace `microservice.local`
3. Port alias: `eureka-server`, port `8761`

This makes `eureka-server.microservice.local:8761` resolv-able by other services in the same namespace.

---

## Step 4 — Deploy Vehicle Service to ECS

### 4a. Create `vehicle-service-task.json`

```json
{
  "family": "vehicle-service-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "vehicle-service",
      "image": "<DOCKER_USERNAME>/car-rental-vehicle-service:latest",
      "portMappings": [
        { "containerPort": 8080, "hostPort": 8080, "protocol": "tcp" }
      ],
      "environment": [
        { "name": "SERVER_PORT",
          "value": "8080" },
        { "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod" },
        { "name": "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE",
          "value": "http://eureka-server.microservice.local:8761/eureka/" },
        { "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:mysql://your-rds-endpoint:3306/vehicle_service?useSSL=true&createDatabaseIfNotExist=true" },
        { "name": "SPRING_DATASOURCE_USERNAME",
          "value": "appuser" },
        { "name": "SPRING_DATASOURCE_PASSWORD",
          "value": "YOUR_DB_PASSWORD" }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 90
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/vehicle-service",
          "awslogs-region": "ap-southeast-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

> **Important:** The `SERVER_PORT=8080` env var here is what overrides the `server.port=0` default in `application.properties`. Without this, Eureka cannot register the service at a predictable port on ECS.

### 4b. Register & Create Vehicle Service

```bash
aws ecs register-task-definition --cli-input-json file://vehicle-service-task.json

aws ecs create-service \
  --cluster microservice-cluster \
  --service-name vehicle-service \
  --task-definition vehicle-service-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}"
```

### 4c. Enable Service Connect

1. **ECS → Services → vehicle-service → Edit**
2. Enable Service Connect → same namespace `microservice.local`
3. Port alias: `vehicle-service`, port `8080`

---

## Step 5 — CI/CD Pipeline Flow

Once secrets are configured, every push to `main`/`master` automatically:

```
1. Build & Test
   └─ mvn package -DskipTests

2. Docker Build & Push
   ├─ Builds image for linux/amd64 (required for ECS Fargate)
   └─ Pushes :latest and :sha-<commit> tags to Docker Hub

3. Deploy to AWS ECS
   ├─ Downloads current ECS task definition
   ├─ Injects these env vars from GitHub Secrets:
   │     SERVER_PORT=8080
   │     SPRING_PROFILES_ACTIVE=prod
   │     EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
   │     SPRING_DATASOURCE_URL
   │     SPRING_DATASOURCE_USERNAME
   │     SPRING_DATASOURCE_PASSWORD
   ├─ Registers a new revision of the task definition
   └─ Updates ECS service → rolling deploy → waits for stability
```

No manual steps needed after the first ECS setup above. ✅

---

## Step 6 — Local Development

```bash
# Start MySQL + vehicle-service locally
docker-compose up --build

# Vehicle service available at:
http://localhost:8081/api/vehicles
```

- Eureka is **disabled** locally (`EUREKA_CLIENT_ENABLED=false` in docker-compose env)
- `SERVER_PORT=8081` is forced by docker-compose, overriding the `0` default
- No Eureka server needed locally ✅

---

## Troubleshooting

**Service not appearing in Eureka dashboard**
- Check `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` uses the correct Service Connect DNS (`*.microservice.local`)
- Security group must allow inbound port `8761` from the vehicle-service security group
- Wait ~30 seconds after the task starts — Eureka registration takes a moment

**ECS tasks keep restarting (health check fails)**
- Confirm `SERVER_PORT=8080` is in the ECS task definition environment
- Check CloudWatch logs: `aws logs tail /ecs/vehicle-service --follow`
- Ensure RDS is reachable from the ECS subnet (check security group / subnet routing)

**`ddl-auto=validate` fails on first deploy**
- The prod profile uses `validate` — the schema must already exist in RDS
- Fix: temporarily set `spring.jpa.hibernate.ddl-auto=update` in `application-prod.properties` for the first deploy, then revert to `validate`

**Port conflict locally**
- Run `lsof -i :8081` and kill the occupying process, or change the host port in `docker-compose.yml`
