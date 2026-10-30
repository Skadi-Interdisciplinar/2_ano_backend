package skadi.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.SignatureRequest;
import skadi.api.dto.SignatureResponse;
import skadi.api.exceptions.SignatureNotFoundException;
import skadi.api.exceptions.SignatureAlreadyExistsException;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.enums.ReportStatus;
import skadi.api.enums.NivelAcesso;
import skadi.api.mapper.SignatureMapper;
import skadi.api.model.Report;
import skadi.api.model.Signature;
import skadi.api.model.User;
import skadi.api.repository.SignatureRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final ReportService reportService;
    private final SignatureRepository repository;
    private final SignatureVerificationService verificationService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public SignatureResponse create(Integer reportId, SignatureRequest request, User user) {
        Integer userId = Math.toIntExact(user.getId());
        auditUserContext.setCurrentUser(user);
        Report report = reportService.findReportForAuthor(reportId, user);
        if (repository.existsByReportId(reportId)) {
            throw new SignatureAlreadyExistsException(reportId);
        }
        if (report.getStatus() != ReportStatus.GERADO) {
            throw new ReportRuleViolationException("Só é possível inserir uma assinatura em um relatório sem assinatura.");
        }
        verificationService.verify(
                reportId,
                report.getContentHash(),
                request.signatureAlgorithm(),
                request.certificatePublicKey(),
                request.signature()
        );

        Signature signature = SignatureMapper.fromDTO(reportId, request);
        Signature savedSignature = repository.save(signature);
        report.setStatus(ReportStatus.ASSINADO);
        return SignatureMapper.toDTO(savedSignature);
    }

    public Optional<SignatureResponse> findByReportId(Integer reportId, User user) {
        Report report = reportService.findReportForUser(reportId, user);
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return repository.findByReportId(reportId).map(SignatureMapper::toDTO);
        }
        return repository.findByReportIdAndReportCdIn(reportId, List.of(report.getDistributionCenterId()))
                .map(SignatureMapper::toDTO);
    }

    public List<SignatureResponse> findAllByCd(User user) {
        reportService.requireReportReadAccess(user);
        List<Signature> signatures;
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            signatures = repository.findAllByOrderByIdDesc();
        } else {
            List<Integer> cdIds = reportService.accessibleCdIds(user);
            signatures = cdIds.isEmpty() ? List.of() : repository.findAllByReportCdIn(cdIds);
        }
        return signatures.stream()
                .map(SignatureMapper::toDTO)
                .toList();
    }

    public SignatureResponse findById(Integer id, User user) {
        reportService.requireReportReadAccess(user);
        Optional<Signature> found;
        if (user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            found = repository.findById(id);
        } else {
            List<Integer> cdIds = reportService.accessibleCdIds(user);
            found = cdIds.isEmpty() ? Optional.empty() : repository.findByIdAndReportCdIn(id, cdIds);
        }
        Signature signature = found
                .orElseThrow(() -> new SignatureNotFoundException(id));
        return SignatureMapper.toDTO(signature);
    }

}
