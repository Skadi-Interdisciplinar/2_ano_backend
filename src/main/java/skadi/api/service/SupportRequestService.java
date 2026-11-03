package skadi.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.SupportRequestRequest;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.SupportRequestUpdateRequest;
import skadi.api.enums.SupportStatus;
import skadi.api.exceptions.SupportRequestNotFoundException;
import skadi.api.exceptions.SupportRequestRuleViolationException;
import skadi.api.mapper.SupportRequestMapper;
import skadi.api.model.SupportRequest;
import skadi.api.model.User;
import skadi.api.repository.SupportRequestRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class SupportRequestService {

    private static final ZoneOffset UTC_MINUS_3 = ZoneOffset.of("-03:00");

    private final SupportRequestRepository repository;

    public SupportRequestService(SupportRequestRepository repository) {
        this.repository = repository;
    }

    public SupportRequestResponse create(SupportRequestRequest request, User solicitante) {
        if (request.status() != null && request.status() != SupportStatus.REGISTRADO) {
            throw new SupportRequestRuleViolationException(
                    "O chamado deve ser aberto com o status REGISTRADO");
        }

        SupportRequest ticket = SupportRequestMapper.fromRequest(request, solicitante, nowUtcMinus3());
        return SupportRequestMapper.toResponse(repository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<SupportRequestResponse> findAll() {
        return SupportRequestMapper.toResponseList(repository.findAll());
    }

    @Transactional(readOnly = true)
    public SupportRequestResponse findById(Long id) {
        return SupportRequestMapper.toResponse(findTicket(id));
    }

    public SupportRequestResponse update(Long id, SupportRequestUpdateRequest request, User responsavel) {
        SupportRequest ticket = findTicket(id);
        SupportRequestMapper.update(ticket, request, responsavel);
        if (ticket.getStatus() == SupportStatus.ATENDIDO) {
            completeAttendedTicket(ticket, responsavel);
        }
        return SupportRequestMapper.toResponse(repository.save(ticket));
    }

    public SupportRequestResponse updateStatus(Long id, SupportStatus status, User responsavel) {
        SupportRequest ticket = findTicket(id);
        SupportStatus currentStatus = ticket.getStatus();
        boolean wasAttended = currentStatus == SupportStatus.ATENDIDO;
        ticket.setStatus(status);
        if (status == SupportStatus.EM_ATENDIMENTO && currentStatus == SupportStatus.REGISTRADO) {
            ticket.setResponsavel(responsavel);
        } else if (status == SupportStatus.ATENDIDO) {
            completeAttendedTicket(ticket, responsavel);
        } else if (wasAttended) {
            ticket.setDataHoraEncerramento(null);
        }
        return SupportRequestMapper.toResponse(repository.save(ticket));
    }

    public void delete(Long id) {
        findTicket(id);
        repository.deleteById(id);
    }

    private void completeAttendedTicket(SupportRequest ticket, User responsavel) {
        if (ticket.getResposta() == null || ticket.getResposta().isBlank()) {
            throw new SupportRequestRuleViolationException(
                    "Uma solicitação atendida deve possuir responsável, resposta e data de encerramento.");
        }
        if (ticket.getResponsavel() == null) {
            ticket.setResponsavel(responsavel);
        }
        if (ticket.getDataHoraEncerramento() == null) {
            ticket.setDataHoraEncerramento(nowUtcMinus3());
        }
    }

    private SupportRequest findTicket(Long id) {
        return repository.findById(id).orElseThrow(SupportRequestNotFoundException::new);
    }

    private LocalDateTime nowUtcMinus3() {
        return LocalDateTime.now(UTC_MINUS_3);
    }
}
