package vehicle_service.api;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController
{

    @PostMapping
    public String SaveVehicle()
    {
        return "Vehicle Saved";
    }

    @GetMapping
    public String getVehicle()
    {
        return "Vehicle get()";
    }

    @PutMapping
    public String updateVehicle()
    {
        return "Vehicle updated";
    }

    @DeleteMapping
    public String deleteVehicle()
    {
        return "Vehicle deleted";
    }

    @GetMapping("/list")
    public String getAllVehicle()
    {
        return "All Vehicle ";
    }

}
