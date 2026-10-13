package skadi.api.mapper;

import skadi.api.dto.BatchRequest;
import skadi.api.dto.BatchResponse;
import skadi.api.enums.BatchStatus;
import skadi.api.model.Batch;
import skadi.api.model.Category;

public final class BatchMapper {

    private BatchMapper() {
    }

    public static Batch fromRequest(BatchRequest request, Category category) {
        return new Batch(
                null,
                request.batchCode().trim(),
                category,
                request.manufacturingDate(),
                request.expirationDate(),
                BatchStatus.ACTIVE
        );
    }

    public static void update(Batch batch, BatchRequest request, Category category) {
        batch.setBatchCode(request.batchCode().trim());
        batch.setCategory(category);
        batch.setManufacturingDate(request.manufacturingDate());
        batch.setExpirationDate(request.expirationDate());
    }

    public static BatchResponse toResponse(Batch batch) {
        return new BatchResponse(
                batch.getId(),
                batch.getBatchCode(),
                batch.getCategory().getId(),
                batch.getCategory().getName(),
                batch.getManufacturingDate(),
                batch.getExpirationDate(),
                batch.getStatus()
        );
    }
}
