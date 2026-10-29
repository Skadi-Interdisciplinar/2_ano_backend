package skadi.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import skadi.api.dto.ReportRequest;
import skadi.api.dto.ReportPatchRequest;
import skadi.api.dto.ReportResponse;
import skadi.api.exceptions.ReportNotFoundException;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.enums.ReportStatus;
import skadi.api.enums.NivelAcesso;
import skadi.api.mapper.ReportMapper;
import skadi.api.model.Report;
import skadi.api.model.User;
import skadi.api.repository.ReportRepository;
import skadi.api.repository.SignatureRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ReportStatus GENERATED = ReportStatus.GERADO;
    private static final ReportStatus SIGNED = ReportStatus.ASSINADO;
    private static final ReportStatus ARCHIVED = ReportStatus.ARQUIVADO;

    private final ReportRepository reportRepository;
    private final SignatureRepository signatureRepository;
    private final AuditUserContext auditUserContext;
    private final DistributionCenterAccessService accessService;

    @Transactional
    public ReportResponse create(ReportRequest request, User user) {
        Integer userId = Math.toIntExact(user.getId());
        auditUserContext.setCurrentUser(user);
        accessService.requireAdminOwnership(user, request.distributionCenterId().longValue());
        validatePeriod(request.periodStart(), request.periodEnd());

        Report report = ReportMapper.fromDTO(request);
        report.setGeneratedByUserId(userId);
        report.setGeneratedAt(LocalDateTime.now());
        report.setContentHash(calculateHash(request.content()));
        report.setStatus(GENERATED);

        return ReportMapper.toDTO(reportRepository.save(report));
    }

    public List<ReportResponse> findAll(User user) {
        requireReportReadAccess(user);
        List<Report> reports;
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            reports = reportRepository.findAll(Sort.by(Sort.Direction.DESC, "generatedAt"));
        } else {
            List<Integer> cdIds = accessibleCdIds(user);
            reports = cdIds.isEmpty() ? List.of() : reportRepository.findAllByDistributionCenterIdIn(
                    cdIds, Sort.by(Sort.Direction.DESC, "generatedAt"));
        }
        return reports.stream()
                .map(ReportMapper::toDTO)
                .toList();
    }

    public ReportResponse findById(Integer id, User user) {
        return ReportMapper.toDTO(findReportForUser(id, user));
    }

    public Report findReportForUser(Integer id, User user) {
        requireReportReadAccess(user);
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return reportRepository.findById(id).orElseThrow(() -> new ReportNotFoundException(id));
        }
        List<Integer> cdIds = accessibleCdIds(user);
        if (cdIds.isEmpty()) {
            throw new ReportNotFoundException(id);
        }
        return reportRepository.findByIdAndDistributionCenterIdIn(id, cdIds)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    public void requireReportReadAccess(User user) {
        if (user == null || (user.getNivelAcesso() != NivelAcesso.ADMIN
                && user.getNivelAcesso() != NivelAcesso.GESTOR
                && user.getNivelAcesso() != NivelAcesso.SUPER_ADMIN)) {
            throw new AccessDeniedException("Este perfil não pode consultar relatórios.");
        }
        if (user.getNivelAcesso() == NivelAcesso.GESTOR && user.getCdId() == null) {
            throw new AccessDeniedException("GESTOR precisa estar vinculado a um CD para consultar relatórios.");
        }
    }

    public List<Integer> accessibleCdIds(User user) {
        return accessService.accessibleDistributionCenterIds(user).stream().map(Math::toIntExact).toList();
    }

    @Transactional
    public ReportResponse update(Integer id, ReportPatchRequest request, User user) {
        auditUserContext.setCurrentUser(user);
        accessService.requireRole(user, NivelAcesso.ADMIN, "Somente ADMIN pode alterar relatórios.");
        if (request.content() == null && request.periodStart() == null && request.periodEnd() == null) {
            throw new ReportRuleViolationException("Informe ao menos um campo para atualizar.");
        }
        if (request.content() != null && request.content().isBlank()) {
            throw new ReportRuleViolationException("O conteúdo do relatório não pode ficar em branco.");
        }

        Report report = findEditableReport(id, user);
        java.time.LocalDate periodStart = request.periodStart() != null
                ? request.periodStart() : report.getPeriodStart();
        java.time.LocalDate periodEnd = request.periodEnd() != null
                ? request.periodEnd() : report.getPeriodEnd();
        validatePeriod(periodStart, periodEnd);

        ReportMapper.updateFromDTO(report, request);
        if (request.content() != null) {
            report.setContentHash(calculateHash(request.content()));
        }
        return ReportMapper.toDTO(reportRepository.save(report));
    }

    @Transactional
    public void delete(Integer id, User user) {
        auditUserContext.setCurrentUser(user);
        accessService.requireRole(user, NivelAcesso.ADMIN, "Somente ADMIN pode excluir relatórios.");
        Report report = findEditableReport(id, user);
        if (signatureRepository.existsByReportId(id)) {
            throw new ReportRuleViolationException("Um relatório com assinaturas não pode ser excluído.");
        }
        reportRepository.delete(report);
    }

    @Transactional
    public ReportResponse updateStatus(Integer id, ReportStatus requestedStatus, User user) {
        auditUserContext.setCurrentUser(user);
        accessService.requireRole(user, NivelAcesso.ADMIN, "Somente ADMIN pode alterar relatórios.");
        Report report = findOwnedReport(id, user);

        if (requestedStatus == ARCHIVED) {
            if (report.getStatus() != SIGNED) {
                throw new ReportRuleViolationException("Somente relatórios assinados podem ser arquivados.");
            }
        } else if (requestedStatus == SIGNED) {
            if (report.getStatus() != ARCHIVED) {
                throw new ReportRuleViolationException("Somente relatórios arquivados podem ser desarquivados.");
            }
        } else {
            throw new ReportRuleViolationException("Transição de status não permitida.");
        }

        report.setStatus(requestedStatus);
        return ReportMapper.toDTO(reportRepository.save(report));
    }

    Report findReportForAuthor(Integer id, Integer userId) {
        return reportRepository.findByIdAndGeneratedByUserId(id, userId)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    public Report findReportForAuthor(Integer id, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode consultar relatórios para assinatura.");
        Report report = findReportForAuthor(id, Math.toIntExact(user.getId()));
        accessService.requireAdminOwnership(user, report.getDistributionCenterId().longValue());
        return report;
    }

    private Report findEditableReport(Integer id, User user) {
        Report report = findOwnedReport(id, user);
        if (GENERATED != report.getStatus()) {
            throw new ReportRuleViolationException("Somente relatórios sem assinatura podem ser alterados ou excluídos.");
        }
        return report;
    }

    private Report findOwnedReport(Integer id, User user) {
        Report report = findReportForAuthor(id, Math.toIntExact(user.getId()));
        accessService.requireAdminOwnership(user, report.getDistributionCenterId().longValue());
        return report;
    }

    private void validatePeriod(java.time.LocalDate start, java.time.LocalDate end) {
        if (start.isAfter(end)) {
            throw new ReportRuleViolationException("A data inicial do período não pode ser posterior à data final.");
        }
    }

    private String calculateHash(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("O algoritmo SHA-256 não está disponível.", ex);
        }
    }

}
