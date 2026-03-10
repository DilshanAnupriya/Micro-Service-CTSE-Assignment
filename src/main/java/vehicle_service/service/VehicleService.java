package vehicle_service.service;

import vehicle_service.dto.request.VehicleRequestDto;
import vehicle_service.dto.response.VehicleResponseDto;
import vehicle_service.dto.response.paginate.VehicleResponsePaginatedDto;

public interface VehicleService {
    public void saveVehicle(VehicleRequestDto dto);
    public void updateVehicle(VehicleRequestDto dto, String id);
    public VehicleResponseDto findVehicleById(String id);
    public void deleteVehicle(String id);
    public VehicleResponsePaginatedDto getAllVehicle(String searchText, int page, int size);

}
