package vehicle_service.dto.response.paginate;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import vehicle_service.dto.response.VehicleResponseDto;

import java.util.List;
@Data
@Getter
@Setter
@Builder
public class VehicleResponsePaginatedDto {
    private Long dataCount;
    private List<VehicleResponseDto> dataList;
}
