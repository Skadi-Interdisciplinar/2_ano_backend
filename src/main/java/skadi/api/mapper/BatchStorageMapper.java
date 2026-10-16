package skadi.api.mapper;

import skadi.api.dto.BatchStorageResponse;
import skadi.api.model.BatchStorage;

public final class BatchStorageMapper {

    private BatchStorageMapper() {
    }

    public static BatchStorageResponse toResponse(BatchStorage storage) {
        return new BatchStorageResponse(
                storage.getId(),
                storage.getBatch().getId(),
                storage.getBatch().getBatchCode(),
                storage.getColdRoom().getId(),
                storage.getColdRoom().getModel(),
                storage.getColdRoom().getLocation(),
                storage.getColdRoom().getDistributionCenter().getId(),
                storage.getColdRoom().getDistributionCenter().getName(),
                storage.getEntryDate(),
                storage.getExitDate(),
                storage.getExitDate() == null
        );
    }
}
