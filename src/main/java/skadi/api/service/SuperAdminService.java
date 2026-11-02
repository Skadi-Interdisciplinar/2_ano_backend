package skadi.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.AdminPerCdDto;
import skadi.api.dto.RecentSupportTicketDto;
import skadi.api.dto.SuperAdminDashboardResponse;
import skadi.api.dto.SuperAdminKpisDto;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;
import skadi.api.exceptions.AccessLevelChangeNotAllowedException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.mapper.UserMapper;
import skadi.api.model.DistributionCenter;
import skadi.api.model.SupportRequest;
import skadi.api.model.User;
import skadi.api.repository.DistributionCenterRepository;
import skadi.api.repository.SupportRequestRepository;
import skadi.api.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SuperAdminService {
    private final UserRepository repository;
    private final DistributionCenterRepository cdRepository;
    private final SupportRequestRepository supportRequestRepository;

    public SuperAdminService(UserRepository repository) {
        this(repository, null, null);
    }

    public SuperAdminService(
            UserRepository repository,
            DistributionCenterRepository cdRepository,
            SupportRequestRepository supportRequestRepository
    ) {
        this.repository = repository;
        this.cdRepository = cdRepository;
        this.supportRequestRepository = supportRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllAdmins() {
        return UserMapper.toResponseList(repository.findAllByNivelAcesso(NivelAcesso.ADMIN));
    }

    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse getDashboardData() {
        List<User> admins = repository.findAllByNivelAcesso(NivelAcesso.ADMIN);
        long totalAdministradores = admins.size();

        List<DistributionCenter> cds = cdRepository != null ? cdRepository.findAll() : List.of();
        long totalCds = cds.size();

        List<SupportRequest> tickets = supportRequestRepository != null ? supportRequestRepository.findAll() : List.of();

        long chamadosRegistrados = 0;
        long chamadosEmAtendimento = 0;
        long chamadosAtendidos = 0;

        Map<SupportStatus, Long> chamadosPorSituacao = new EnumMap<>(SupportStatus.class);
        for (SupportStatus status : SupportStatus.values()) {
            chamadosPorSituacao.put(status, 0L);
        }

        Map<SupportCategory, Long> chamadosPorCategoria = new EnumMap<>(SupportCategory.class);
        for (SupportCategory category : SupportCategory.values()) {
            chamadosPorCategoria.put(category, 0L);
        }

        for (SupportRequest ticket : tickets) {
            if (ticket.getStatus() != null) {
                chamadosPorSituacao.put(ticket.getStatus(), chamadosPorSituacao.get(ticket.getStatus()) + 1);
                if (ticket.getStatus() == SupportStatus.REGISTRADO) {
                    chamadosRegistrados++;
                } else if (ticket.getStatus() == SupportStatus.EM_ATENDIMENTO) {
                    chamadosEmAtendimento++;
                } else if (ticket.getStatus() == SupportStatus.ATENDIDO) {
                    chamadosAtendidos++;
                }
            }

            if (ticket.getCategoria() != null) {
                chamadosPorCategoria.put(ticket.getCategoria(), chamadosPorCategoria.get(ticket.getCategoria()) + 1);
            }
        }

        long chamadosAbertos = chamadosRegistrados + chamadosEmAtendimento;

        SuperAdminKpisDto kpis = new SuperAdminKpisDto(
                totalAdministradores,
                totalCds,
                chamadosAbertos,
                chamadosRegistrados,
                chamadosEmAtendimento,
                chamadosAtendidos
        );

        List<AdminPerCdDto> administradoresPorCd = cds.stream().map(cd -> {
            long adminsCount = admins.stream()
                    .filter(a -> a.getCd() != null && a.getCd().stream().anyMatch(c -> c.getId().equals(cd.getId())))
                    .count();
            return new AdminPerCdDto(String.valueOf(cd.getId()), cd.getName(), adminsCount);
        }).toList();

        List<RecentSupportTicketDto> chamadosRecentes = tickets.stream()
                .filter(t -> t.getDataHoraAbertura() != null)
                .sorted(Comparator.comparing(SupportRequest::getDataHoraAbertura).reversed())
                .limit(10)
                .map(t -> new RecentSupportTicketDto(
                        String.valueOf(t.getId()),
                        t.getSolicitante() != null && t.getSolicitante().getId() != null
                                ? String.valueOf(t.getSolicitante().getId())
                                : null,
                        t.getSolicitante() != null ? t.getSolicitante().getNome() : null,
                        t.getCategoria(),
                        t.getDescricao(),
                        t.getStatus(),
                        t.getDataHoraAbertura(),
                        t.getDataHoraEncerramento(),
                        t.getResposta(),
                        t.getResponsavel() != null && t.getResponsavel().getId() != null
                                ? String.valueOf(t.getResponsavel().getId())
                                : null,
                        t.getResponsavel() != null ? t.getResponsavel().getNome() : null
                ))
                .toList();

        return new SuperAdminDashboardResponse(
                kpis,
                chamadosPorSituacao,
                chamadosPorCategoria,
                administradoresPorCd,
                chamadosRecentes,
                LocalDateTime.now()
        );
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (request.nivelAcesso() != user.getNivelAcesso()) {
            throw new AccessLevelChangeNotAllowedException();
        }

        UserMapper.update(user, request);
        if (user.getGestor() != null && user.getGestor().getId() != null) {
            user.setGestor(repository.getReferenceById(user.getGestor().getId()));
        }
        user.validateGestorAssociation();
        return UserMapper.toResponse(repository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
        if (user.getCd() != null) {
            user.getCd().clear();
        }
        repository.findAllByGestor_Id(id).forEach(linked -> linked.setGestor(null));
        repository.delete(user);
    }
}
