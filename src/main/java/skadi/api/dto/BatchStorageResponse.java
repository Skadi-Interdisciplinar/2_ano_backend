package skadi.api.dto;

import java.time.LocalDateTime;

public record BatchStorageResponse(
        Long id,
        Long batchId,
        String batchCode,
        Long coldRoomId,
        String coldRoomModel,
        String coldRoomLocation,
        Long distributionCenterId,
        String distributionCenterName,
        LocalDateTime entryDate,
        LocalDateTime exitDate,
        boolean active
) {
}
