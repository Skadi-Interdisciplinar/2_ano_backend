package skadi.api.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import skadi.api.enums.BatchStatus;

@Converter
public class BatchStatusConverter implements AttributeConverter<BatchStatus, String> {

    @Override
    public String convertToDatabaseColumn(BatchStatus status) {
        return status == null ? null : status.getDatabaseValue();
    }

    @Override
    public BatchStatus convertToEntityAttribute(String value) {
        return value == null ? null : BatchStatus.fromDatabaseValue(value);
    }
}
