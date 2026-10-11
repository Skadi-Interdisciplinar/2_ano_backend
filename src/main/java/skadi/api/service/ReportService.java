package skadi.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.ReportRequest;
import skadi.api.dto.ReportResponse;
import skadi.api.exceptions.ReportNotFoundException;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.enums.ReportStatus;
import skadi.api.mapper.ReportMapper;
import skadi.api.model.Report;
import skadi.api.repository.ReportRepository;
import skadi.api.repository.SignatureRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ReportStatus GENERATED = ReportStatus.GERADO;
    private static final ReportStatus SIGNED = ReportStatus.ASSINADO;
    private static final ReportStatus ARCHIVED = ReportStatus.ARQUIVADO;

    private final ReportRepository reportRepository;
    private final SignatureRepository signatureRepository;

    @Transactional
    public ReportResponse create(ReportRequest request, Integer userId) {
        validatePeriod(request.periodStart(), request.periodEnd());

        Report report = ReportMapper.fromDTO(request);
        report.setGeneratedByUserId(userId);
        report.setGeneratedAt(LocalDateTime.now());
        report.setContentHash(calculateHash(request.content()));
        report.setStatus(GENERATED);

        return ReportMapper.toDTO(reportRepository.save(report));
    }

    public List<ReportResponse> findAll() {
        return reportRepository.findAllByOrderByGeneratedAtDesc().stream()
                .map(ReportMapper::toDTO)
                .toList();
    }

    public ReportResponse findById(Integer id) {
        return ReportMapper.toDTO(findReport(id));
    }

    @Transactional
    public ReportResponse update(Integer id, ReportRequest request, Integer userId) {
        validatePeriod(request.periodStart(), request.periodEnd());
        Report report = findEditableReport(id, userId);
        ReportMapper.updateFromDTO(report, request);
        report.setContentHash(calculateHash(request.content()));
        return ReportMapper.toDTO(reportRepository.save(report));
    }

    @Transactional
    public void delete(Integer id, Integer userId) {
        Report report = findEditableReport(id, userId);
        if (signatureRepository.existsByReportId(id)) {
            throw new ReportRuleViolationException("Um relatório com assinaturas não pode ser excluído.");
        }
        reportRepository.delete(report);
    }

    @Transactional
    public ReportResponse updateStatus(Integer id, ReportStatus requestedStatus, Integer userId) {
        Report report = findReportForAuthor(id, userId);

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

    Report findReport(Integer id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    Report findReportForAuthor(Integer id, Integer userId) {
        return reportRepository.findByIdAndGeneratedByUserId(id, userId)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    private Report findEditableReport(Integer id, Integer userId) {
        Report report = findReportForAuthor(id, userId);
        if (GENERATED != report.getStatus()) {
            throw new ReportRuleViolationException("Somente relatórios sem assinatura podem ser alterados ou excluídos.");
        }
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
