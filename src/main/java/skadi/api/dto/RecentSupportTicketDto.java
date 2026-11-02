package skadi.api.dto;

import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;

import java.time.LocalDateTime;

public record RecentSupportTicketDto(
        String id,
        String solicitanteId,
        String solicitanteNome,
        SupportCategory categoria,
        String descricao,
        SupportStatus status,
        LocalDateTime dataHoraAbertura,
        LocalDateTime dataHoraEncerramento,
        String resposta,
        String responsavelId,
        String responsavelNome
) {
}
