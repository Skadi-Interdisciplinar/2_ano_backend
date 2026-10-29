package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.SupportRequestRequest;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.SupportRequestUpdateRequest;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;
import skadi.api.exceptions.SupportRequestNotFoundException;
import skadi.api.exceptions.SupportRequestRuleViolationException;
import skadi.api.model.SupportRequest;
import skadi.api.model.User;
import skadi.api.repository.SupportRequestRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

    private static final ZoneOffset UTC_MINUS_3 = ZoneOffset.of("-03:00");

    @Mock
    private SupportRequestRepository repository;

    private SupportRequestService service;

    @BeforeEach
    void setUp() {
        service = new SupportRequestService(repository);
    }

    @Test
    void createOpensTicketAsRegistradoWithUtcMinus3() {
        when(repository.save(any(SupportRequest.class))).thenAnswer(invocation -> {
            SupportRequest ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });
        LocalDateTime before = LocalDateTime.now(UTC_MINUS_3).minusSeconds(2);

        SupportRequestResponse response = service.create(openRequest(null), user(4L));

        LocalDateTime after = LocalDateTime.now(UTC_MINUS_3).plusSeconds(2);
        assertEquals(1L, response.id());
        assertEquals(4L, response.codUsuarioSolicitante());
        assertEquals(SupportCategory.DUVIDA, response.categoria());
        assertEquals("Não consigo abrir o relatório.", response.descricao());
        assertEquals(SupportStatus.REGISTRADO, response.status());
        assertFalse(response.dataHoraAbertura().isBefore(before));
        assertFalse(response.dataHoraAbertura().isAfter(after));
        assertNull(response.dataHoraEncerramento());
        assertNull(response.resposta());
        assertNull(response.codUsuarioResponsavel());
    }

    @Test
    void createAcceptsExplicitRegistradoStatus() {
        when(repository.save(any(SupportRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportRequestResponse response = service.create(openRequest(SupportStatus.REGISTRADO), user(4L));

        assertEquals(SupportStatus.REGISTRADO, response.status());
    }

    @Test
    void createRejectsStatusOtherThanRegistrado() {
        assertThrows(SupportRequestRuleViolationException.class,
                () -> service.create(openRequest(SupportStatus.EM_ATENDIMENTO), user(4L)));

        verify(repository, never()).save(any());
    }

    @Test
    void findByIdReturnsTicket() {
        when(repository.findById(1L)).thenReturn(Optional.of(openTicket()));

        SupportRequestResponse response = service.findById(1L);

        assertEquals(1L, response.id());
        assertEquals(SupportStatus.REGISTRADO, response.status());
        assertEquals(4L, response.codUsuarioSolicitante());
    }

    @Test
    void findByIdRejectsUnknownTicket() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        SupportRequestNotFoundException exception = assertThrows(SupportRequestNotFoundException.class,
                () -> service.findById(99L));

        assertEquals("Chamado não encontrado", exception.getMessage());
    }

    @Test
    void findAllReturnsTickets() {
        when(repository.findAll()).thenReturn(List.of(openTicket()));

        List<SupportRequestResponse> response = service.findAll();

        assertEquals(1, response.size());
        assertEquals(1L, response.getFirst().id());
    }

    @Test
    void updateChangesEditableFieldsAndAssignsResponsibleWhenThereIsAnAnswer() {
        SupportRequest ticket = openTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime abertura = ticket.getDataHoraAbertura();

        SupportRequestResponse response = service.update(1L, updateRequest("Estamos verificando."), user(9L));

        assertEquals(SupportCategory.RECLAMACAO, response.categoria());
        assertEquals("O relatório continua indisponível.", response.descricao());
        assertEquals("Estamos verificando.", response.resposta());
        assertEquals(9L, response.codUsuarioResponsavel());
        assertEquals(SupportStatus.REGISTRADO, response.status());
        assertEquals(abertura, response.dataHoraAbertura());
        assertNull(response.dataHoraEncerramento());
    }

    @Test
    void updateWithoutAnswerKeepsResponsible() {
        SupportRequest ticket = openTicket();
        ticket.setResponsavel(user(9L));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenAnswer(invocation -> invocation.getArgument(0));

        SupportRequestResponse response = service.update(
                1L,
                new SupportRequestUpdateRequest(SupportCategory.DUVIDA, "Texto", null),
                user(2L)
        );

        assertNull(response.resposta());
        assertEquals(9L, response.codUsuarioResponsavel());
    }

    @Test
    void updateStatusToAttendedFillsClosingTimeInUtcMinus3() {
        SupportRequest ticket = openTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime before = LocalDateTime.now(UTC_MINUS_3).minusSeconds(2);

        SupportRequestResponse response = service.updateStatus(1L, SupportStatus.ATENDIDO, user(9L));

        LocalDateTime after = LocalDateTime.now(UTC_MINUS_3).plusSeconds(2);
        assertEquals(SupportStatus.ATENDIDO, response.status());
        assertEquals(9L, response.codUsuarioResponsavel());
        assertFalse(response.dataHoraEncerramento().isBefore(before));
        assertFalse(response.dataHoraEncerramento().isAfter(after));
        assertFalse(response.dataHoraEncerramento().isBefore(response.dataHoraAbertura()));
        assertEquals(ticket.getDataHoraAbertura(), response.dataHoraAbertura());
    }

    @Test
    void updateStatusToInProgressDoesNotFillClosingTime() {
        SupportRequest ticket = openTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenAnswer(invocation -> invocation.getArgument(0));

        SupportRequestResponse response = service.updateStatus(1L, SupportStatus.EM_ATENDIMENTO, user(9L));

        assertEquals(SupportStatus.EM_ATENDIMENTO, response.status());
        assertNull(response.dataHoraEncerramento());
        assertNull(response.codUsuarioResponsavel());
    }

    @Test
    void updateStatusLeavingAttendedClearsClosingTime() {
        SupportRequest ticket = openTicket();
        ticket.setStatus(SupportStatus.ATENDIDO);
        ticket.setDataHoraEncerramento(LocalDateTime.of(2026, 10, 29, 9, 0));
        ticket.setResponsavel(user(9L));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenAnswer(invocation -> invocation.getArgument(0));

        SupportRequestResponse response = service.updateStatus(1L, SupportStatus.EM_ATENDIMENTO, user(2L));

        assertEquals(SupportStatus.EM_ATENDIMENTO, response.status());
        assertNull(response.dataHoraEncerramento());
        assertEquals(9L, response.codUsuarioResponsavel());
    }

    @Test
    void updateStatusWhenAlreadyAttendedKeepsClosingTime() {
        SupportRequest ticket = openTicket();
        LocalDateTime encerramento = LocalDateTime.of(2026, 10, 29, 9, 0);
        ticket.setStatus(SupportStatus.ATENDIDO);
        ticket.setDataHoraEncerramento(encerramento);
        ticket.setResponsavel(user(9L));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenAnswer(invocation -> invocation.getArgument(0));

        SupportRequestResponse response = service.updateStatus(1L, SupportStatus.ATENDIDO, user(2L));

        assertEquals(SupportStatus.ATENDIDO, response.status());
        assertEquals(encerramento, response.dataHoraEncerramento());
        assertEquals(9L, response.codUsuarioResponsavel());
    }

    @Test
    void updateStatusRejectsUnknownTicket() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupportRequestNotFoundException.class,
                () -> service.updateStatus(99L, SupportStatus.ATENDIDO, user(9L)));

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsUnknownTicket() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupportRequestNotFoundException.class,
                () -> service.update(99L, updateRequest("ok"), user(9L)));

        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingTicket() {
        when(repository.findById(1L)).thenReturn(Optional.of(openTicket()));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteRejectsUnknownTicket() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        SupportRequestNotFoundException exception = assertThrows(SupportRequestNotFoundException.class,
                () -> service.delete(99L));

        assertEquals("Chamado não encontrado", exception.getMessage());
        verify(repository, never()).deleteById(any());
    }

    private SupportRequestRequest openRequest(SupportStatus status) {
        return new SupportRequestRequest(
                SupportCategory.DUVIDA,
                "Não consigo abrir o relatório.",
                status
        );
    }

    private SupportRequestUpdateRequest updateRequest(String resposta) {
        return new SupportRequestUpdateRequest(
                SupportCategory.RECLAMACAO,
                "O relatório continua indisponível.",
                resposta
        );
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
