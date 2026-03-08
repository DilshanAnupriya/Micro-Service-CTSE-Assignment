package vehicle_service.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vehicle_service.entity.Vehicle;

public interface  VehicleRepo extends JpaRepository<Vehicle, String> {
    @Query(nativeQuery = true,value = "SELECT * FROM vehicle WHERE brand LIKE %?1% OR model LIKE %?1%")
    public Page<Vehicle> searchAllVehicle(String searchText, Pageable pageable);

    @Query(nativeQuery = true,value = "SELECT COUNT(*) FROM vehicle WHERE brand LIKE %?1% OR model LIKE %?1%")
    public Long countAllVehicle(String searchText);
}
