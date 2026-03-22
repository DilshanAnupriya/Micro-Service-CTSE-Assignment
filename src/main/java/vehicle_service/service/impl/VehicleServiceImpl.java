package vehicle_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vehicle_service.dto.request.VehicleRequestDto;
import vehicle_service.dto.response.VehicleResponseDto;
import vehicle_service.dto.response.paginate.VehicleResponsePaginatedDto;
import vehicle_service.entity.Vehicle;
import vehicle_service.exception.EntryNotFoundException;
import vehicle_service.repo.VehicleRepo;
import vehicle_service.service.VehicleService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepo vehicleRepo;
    private final vehicle_service.kafka.VehicleEventProducer eventProducer;

    @Override
    public void saveVehicle(VehicleRequestDto dto) {
        Vehicle vehicle = toVehicle(dto);
        vehicleRepo.save(vehicle);
        eventProducer.publishVehicleCreated(vehicle);
    }

    @Override
    public void updateVehicle(VehicleRequestDto dto, String id) {
        Vehicle vehicle = vehicleRepo.findById(id)
                .orElseThrow(()->new EntryNotFoundException("Vehicle not found"));
        vehicle.setMake(dto.getMake());
        vehicle.setModel(dto.getModel());
        vehicle.setColor(dto.getColor());
        vehicle.setMileage(dto.getMileage());
        vehicle.setYear(dto.getYear());
        vehicle.setPlateNumber(dto.getPlateNumber());
        vehicle.setDailyRate(dto.getDailyRate());
        vehicle.setImageUrl(dto.getImageUrl());
        vehicle.setDescription(dto.getDescription());
        vehicleRepo.save(vehicle);
        
        eventProducer.publishVehicleUpdated(vehicle);

    }

    @Override
    public VehicleResponseDto findVehicleById(String id) {
        return toVehicleResponseDto(vehicleRepo.findById(id)
                .orElseThrow(()->new EntryNotFoundException("Vehicle not found"))
        );
    }

    @Override
    public void deleteVehicle(String id) {
        vehicleRepo.deleteById(id);
        eventProducer.publishVehicleDeleted(id);
    }

    @Override
    public VehicleResponsePaginatedDto getAllVehicle(String searchText, int page, int size) {
        return  VehicleResponsePaginatedDto.builder()
                .dataCount(vehicleRepo.countAllVehicle(searchText))
                .dataList(
                        vehicleRepo.searchAllVehicle(searchText, PageRequest.of(page,size))
                                .stream().map(this::toVehicleResponseDto).collect(Collectors.toList())
                )
                .build();
    }

    private Vehicle toVehicle(VehicleRequestDto dto){
        if(dto==null)throw new EntryNotFoundException("Vehicle is null");
        return Vehicle.builder()
                .id(UUID.randomUUID().toString())
                .make(dto.getMake())
                .brand(dto.getBrand())
                .model(dto.getModel())
                .color(dto.getColor())
                .mileage(dto.getMileage())
                .year(dto.getYear())
                .plateNumber(dto.getPlateNumber())
                .updatedAt(LocalDateTime.now())
                .imageUrl(dto.getImageUrl())
                .dailyRate(dto.getDailyRate())
                .description(dto.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private VehicleResponseDto toVehicleResponseDto(Vehicle vehicle){
        if(vehicle==null)throw new EntryNotFoundException("Vehicle is null");
        return VehicleResponseDto.builder()
                .id(vehicle.getId())
                .brand(vehicle.getBrand())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .mileage(vehicle.getMileage())
                .year(vehicle.getYear())
                .plateNumber(vehicle.getPlateNumber())
                .updatedAt(vehicle.getUpdatedAt())
                .imageUrl(vehicle.getImageUrl())
                .dailyRate(vehicle.getDailyRate())
                .description(vehicle.getDescription())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}
