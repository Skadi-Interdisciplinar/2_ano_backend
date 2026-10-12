package skadi.api.converter;

import org.junit.jupiter.api.Test;
import skadi.api.enums.BatchStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BatchStatusConverterTest {

    private final BatchStatusConverter converter = new BatchStatusConverter();

    @Test
    void convertsEnglishApiStatusToPortugueseDatabaseValue() {
        assertEquals("ativo", converter.convertToDatabaseColumn(BatchStatus.ACTIVE));
        assertEquals("bloqueado", converter.convertToDatabaseColumn(BatchStatus.BLOCKED));
        assertEquals("expedido", converter.convertToDatabaseColumn(BatchStatus.SHIPPED));
        assertEquals("vencido", converter.convertToDatabaseColumn(BatchStatus.EXPIRED));
    }

    @Test
    void convertsPortugueseDatabaseValueToEnglishApiStatus() {
        assertEquals(BatchStatus.ACTIVE, converter.convertToEntityAttribute("ativo"));
        assertEquals(BatchStatus.BLOCKED, converter.convertToEntityAttribute("bloqueado"));
        assertEquals(BatchStatus.SHIPPED, converter.convertToEntityAttribute("expedido"));
        assertEquals(BatchStatus.EXPIRED, converter.convertToEntityAttribute("vencido"));
    }

    @Test
    void preservesNullValues() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
