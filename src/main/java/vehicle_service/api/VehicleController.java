package vehicle_service.api;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vehicle_service.dto.request.VehicleRequestDto;
import vehicle_service.service.VehicleService;
import vehicle_service.util.StandardResponseDto;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController
{
    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<StandardResponseDto> SaveVehicle(
            @RequestBody VehicleRequestDto vehicleRequestDto
    )
    {
        vehicleService.saveVehicle(vehicleRequestDto);
        return new ResponseEntity<>(
                StandardResponseDto.builder()
                        .code(201)
                        .message("Vehicle Saved !")
                        .data(null)
                        .build(),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandardResponseDto> getVehicle(
            @PathVariable String id
    )
    {
        return new ResponseEntity<>(
                StandardResponseDto.builder()
                        .code(200)
                        .message("Vehicle data !")
                        .data(vehicleService.findVehicleById(id))
                        .build(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<StandardResponseDto> updateVehicle(
            @PathVariable String id,
            @RequestBody VehicleRequestDto vehicleRequestDto
    )
    {
        vehicleService.updateVehicle(vehicleRequestDto,id);
        return new ResponseEntity<>(
                StandardResponseDto.builder()
                        .code(201)
                        .message("Vehicle Updated !")
                        .data(null)
                        .build(),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponseDto> deleteVehicle(
            @PathVariable String id
    )
    {
        vehicleService.deleteVehicle(id);
        return new ResponseEntity<>(
                StandardResponseDto.builder()
                        .code(204)
                        .message("Vehicle Deleted !")
                        .data(null)
                        .build(),
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/list")
    public ResponseEntity<StandardResponseDto> getAllVehicle(
           @RequestParam String searchText,
           @RequestParam int Page,
           @RequestParam int Size
    )

    {

        return new ResponseEntity<>(
                StandardResponseDto.builder()
                        .code(201)
                        .message("Vehicle List !")
                        .data( vehicleService.getAllVehicle(searchText,Page,Size))
                        .build(),
                HttpStatus.OK
        );
    }

}
