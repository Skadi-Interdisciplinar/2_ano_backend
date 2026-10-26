package skadi.api.converter;

import org.junit.jupiter.api.Test;
import skadi.api.enums.BatchStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BatchStatusConverterTest {

    private final BatchStatusConverter converter = new BatchStatusConverter();

    @Test
    void convertsEnglishApiStatusToPortugueseDatabaseValue() {
        assertEquals("ATIVO", converter.convertToDatabaseColumn(BatchStatus.ACTIVE));
        assertEquals("BLOQUEADO", converter.convertToDatabaseColumn(BatchStatus.BLOCKED));
        assertEquals("EXPEDIDO", converter.convertToDatabaseColumn(BatchStatus.SHIPPED));
        assertEquals("VENDIDO", converter.convertToDatabaseColumn(BatchStatus.SOLD));
    }

    @Test
    void convertsPortugueseDatabaseValueToEnglishApiStatus() {
        assertEquals(BatchStatus.ACTIVE, converter.convertToEntityAttribute("ATIVO"));
        assertEquals(BatchStatus.BLOCKED, converter.convertToEntityAttribute("BLOQUEADO"));
        assertEquals(BatchStatus.SHIPPED, converter.convertToEntityAttribute("EXPEDIDO"));
        assertEquals(BatchStatus.SOLD, converter.convertToEntityAttribute("VENDIDO"));
    }

    @Test
    void preservesNullValues() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
