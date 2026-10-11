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
import skadi.api.mapper.SignatureMapper;
import skadi.api.model.Report;
import skadi.api.model.Signature;
import skadi.api.repository.SignatureRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final ReportService reportService;
    private final SignatureRepository repository;
    private final SignatureVerificationService verificationService;

    @Transactional
    public SignatureResponse create(Integer reportId, SignatureRequest request, Integer userId) {
        Report report = reportService.findReportForAuthor(reportId, userId);
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

    public List<SignatureResponse> findByReportId(Integer reportId, Integer userId) {
        reportService.findReportForAuthor(reportId, userId);
        return repository.findByReportIdOrderByIdDesc(reportId).stream()
                .map(SignatureMapper::toDTO)
                .toList();
    }

    public SignatureResponse findById(Integer id, Integer userId) {
        Signature signature = findSignature(id);
        reportService.findReportForAuthor(signature.getReportId(), userId);
        return SignatureMapper.toDTO(signature);
    }

    private Signature findSignature(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new SignatureNotFoundException(id));
    }

}
