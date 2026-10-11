package skadi.api.dto;

import java.math.BigDecimal;

public record CategoryResponse(
        Long id,
        String nome,
        BigDecimal temperaturaIdeal,
        BigDecimal vidaUtilHoras
) {
}
