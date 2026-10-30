package skadi.api.mapper;

import skadi.api.dto.SupportRequestRequest;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.SupportRequestUpdateRequest;
import skadi.api.enums.SupportStatus;
import skadi.api.model.SupportRequest;
import skadi.api.model.User;

import java.time.LocalDateTime;
import java.util.List;

public final class SupportRequestMapper {

    private SupportRequestMapper() {
    }

    public static SupportRequest fromRequest(
            SupportRequestRequest request,
            User solicitante,
            LocalDateTime dataHoraAbertura
    ) {
        SupportRequest ticket = new SupportRequest();
        ticket.setSolicitante(solicitante);
        ticket.setCategoria(request.categoria());
        ticket.setDescricao(request.descricao());
        ticket.setStatus(SupportStatus.REGISTRADO);
        ticket.setDataHoraAbertura(dataHoraAbertura);
        ticket.setDataHoraEncerramento(null);
        ticket.setResposta(null);
        ticket.setResponsavel(null);
        return ticket;
    }

    public static void update(SupportRequest ticket, SupportRequestUpdateRequest request, User responsavel) {
        ticket.setCategoria(request.categoria());
        ticket.setDescricao(request.descricao());
        ticket.setResposta(request.resposta());
        if (request.resposta() != null && !request.resposta().isBlank()) {
            ticket.setResponsavel(responsavel);
        }
    }

    public static SupportRequestResponse toResponse(SupportRequest ticket) {
        return new SupportRequestResponse(
                ticket.getId(),
                ticket.getSolicitante().getId(),
                ticket.getCategoria(),
                ticket.getDescricao(),
                ticket.getStatus(),
                ticket.getDataHoraAbertura(),
                ticket.getDataHoraEncerramento(),
                ticket.getResposta(),
                ticket.getResponsavel() == null ? null : ticket.getResponsavel().getId()
        );
    }

    public static List<SupportRequestResponse> toResponseList(List<SupportRequest> tickets) {
        return tickets.stream().map(SupportRequestMapper::toResponse).toList();
    }
}
