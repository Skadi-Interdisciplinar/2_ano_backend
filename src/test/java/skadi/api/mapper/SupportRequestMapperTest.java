package skadi.api.mapper;

import org.junit.jupiter.api.Test;
import skadi.api.dto.SupportRequestRequest;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.SupportRequestUpdateRequest;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;
import skadi.api.model.SupportRequest;
import skadi.api.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SupportRequestMapperTest {

    @Test
    void fromRequestOpensTicketAsRegistradoAndIgnoresIncomingStatus() {
        User solicitante = user(4L);
        LocalDateTime abertura = LocalDateTime.of(2026, 10, 29, 8, 25);
        SupportRequestRequest request = new SupportRequestRequest(
                SupportCategory.DUVIDA,
                "Não consigo abrir o relatório.",
                SupportStatus.EM_ATENDIMENTO
        );

        SupportRequest ticket = SupportRequestMapper.fromRequest(request, solicitante, abertura);

        assertNull(ticket.getId());
        assertSame(solicitante, ticket.getSolicitante());
        assertEquals(SupportCategory.DUVIDA, ticket.getCategoria());
        assertEquals("Não consigo abrir o relatório.", ticket.getDescricao());
        assertEquals(SupportStatus.REGISTRADO, ticket.getStatus());
        assertEquals(abertura, ticket.getDataHoraAbertura());
        assertNull(ticket.getDataHoraEncerramento());
        assertNull(ticket.getResposta());
        assertNull(ticket.getResponsavel());
    }

    @Test
    void updateAppliesEditableFieldsAndAssignsResponsibleWhenThereIsAnAnswer() {
        SupportRequest ticket = openTicket();
        User responsavel = user(9L);
        LocalDateTime abertura = ticket.getDataHoraAbertura();

        SupportRequestMapper.update(
                ticket,
                new SupportRequestUpdateRequest(
                        SupportCategory.RECLAMACAO,
                        "O relatório continua indisponível.",
                        "Estamos verificando."
                ),
                responsavel
        );

        assertEquals(SupportCategory.RECLAMACAO, ticket.getCategoria());
        assertEquals("O relatório continua indisponível.", ticket.getDescricao());
        assertEquals("Estamos verificando.", ticket.getResposta());
        assertSame(responsavel, ticket.getResponsavel());
        assertEquals(SupportStatus.REGISTRADO, ticket.getStatus());
        assertEquals(abertura, ticket.getDataHoraAbertura());
        assertNull(ticket.getDataHoraEncerramento());
        assertEquals(4L, ticket.getSolicitante().getId());
    }

    @Test
    void updateWithoutAnswerKeepsResponsible() {
        User responsavel = user(9L);
        SupportRequest ticket = openTicket();
        ticket.setResponsavel(responsavel);

        SupportRequestMapper.update(
                ticket,
                new SupportRequestUpdateRequest(SupportCategory.DUVIDA, "Texto", "   "),
                user(2L)
        );

        assertEquals("   ", ticket.getResposta());
        assertSame(responsavel, ticket.getResponsavel());
    }

    @Test
    void toResponseMapsTicketWithoutResponsible() {
        SupportRequestResponse response = SupportRequestMapper.toResponse(openTicket());

        assertEquals(1L, response.id());
        assertEquals(4L, response.codUsuarioSolicitante());
        assertEquals(SupportCategory.DUVIDA, response.categoria());
        assertEquals("Não consigo abrir o relatório.", response.descricao());
        assertEquals(SupportStatus.REGISTRADO, response.status());
        assertEquals(LocalDateTime.of(2026, 10, 29, 8, 25), response.dataHoraAbertura());
        assertNull(response.dataHoraEncerramento());
        assertNull(response.resposta());
        assertNull(response.codUsuarioResponsavel());
    }

    @Test
    void toResponseMapsResponsibleId() {
        SupportRequest ticket = openTicket();
        ticket.setStatus(SupportStatus.ATENDIDO);
        ticket.setResposta("Resolvido.");
        ticket.setDataHoraEncerramento(LocalDateTime.of(2026, 10, 29, 9, 0));
        ticket.setResponsavel(user(9L));

        SupportRequestResponse response = SupportRequestMapper.toResponse(ticket);

        assertEquals(SupportStatus.ATENDIDO, response.status());
        assertEquals("Resolvido.", response.resposta());
        assertEquals(LocalDateTime.of(2026, 10, 29, 9, 0), response.dataHoraEncerramento());
        assertEquals(9L, response.codUsuarioResponsavel());
    }

    @Test
    void toResponseListMapsEveryTicket() {
        SupportRequest second = openTicket();
        second.setId(2L);

        List<SupportRequestResponse> response = SupportRequestMapper.toResponseList(List.of(openTicket(), second));

        assertEquals(List.of(1L, 2L), response.stream().map(SupportRequestResponse::id).toList());
    }

    private SupportRequest openTicket() {
        SupportRequest ticket = new SupportRequest();
        ticket.setId(1L);
        ticket.setSolicitante(user(4L));
        ticket.setCategoria(SupportCategory.DUVIDA);
        ticket.setDescricao("Não consigo abrir o relatório.");
        ticket.setStatus(SupportStatus.REGISTRADO);
        ticket.setDataHoraAbertura(LocalDateTime.of(2026, 10, 29, 8, 25));
        return ticket;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
