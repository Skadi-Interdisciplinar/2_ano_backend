package skadi.api.dto;

import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;

import java.time.LocalDateTime;

public record SupportRequestResponse(
        Long id,
        Long codUsuarioSolicitante,
        String solicitanteNome,
        SupportCategory categoria,
        String descricao,
        SupportStatus status,
        LocalDateTime dataHoraAbertura,
        LocalDateTime dataHoraEncerramento,
        String resposta,
        Long codUsuarioResponsavel,
        String responsavelNome
) {
    public SupportRequestResponse(
            Long id,
            Long codUsuarioSolicitante,
            SupportCategory categoria,
            String descricao,
            SupportStatus status,
            LocalDateTime dataHoraAbertura,
            LocalDateTime dataHoraEncerramento,
            String resposta,
            Long codUsuarioResponsavel
    ) {
        this(
                id,
                codUsuarioSolicitante,
                null,
                categoria,
                descricao,
                status,
                dataHoraAbertura,
                dataHoraEncerramento,
                resposta,
                codUsuarioResponsavel,
                null
        );
    }
}
