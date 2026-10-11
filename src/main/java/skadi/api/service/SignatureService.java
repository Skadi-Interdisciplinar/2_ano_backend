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
import skadi.api.model.Report;
import skadi.api.model.Signature;
import skadi.api.repository.SignatureRepository;

import java.time.LocalDate;
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

        Signature signature = new Signature();
        signature.setReportId(reportId);
        signature.setCertificateHolder(request.certificateHolder());
        signature.setSerialNumber(request.serialNumber());
        signature.setCertifyingAuthority(request.certifyingAuthority());
        signature.setSignatureAlgorithm(request.signatureAlgorithm());
        signature.setCertificatePublicKey(request.certificatePublicKey());
        signature.setSignature(request.signature());
        signature.setTimestampToken(request.timestampToken());
        signature.setSignedAt(LocalDate.now());
        Signature savedSignature = repository.save(signature);
        report.setStatus(ReportStatus.ASSINADO);
        return toResponse(savedSignature);
    }

    public List<SignatureResponse> findByReportId(Integer reportId, Integer userId) {
        reportService.findReportForAuthor(reportId, userId);
        return repository.findByReportIdOrderByIdDesc(reportId).stream()
                .map(this::toResponse)
                .toList();
    }

    public SignatureResponse findById(Integer id, Integer userId) {
        Signature signature = findSignature(id);
        reportService.findReportForAuthor(signature.getReportId(), userId);
        return toResponse(signature);
    }

    private Signature findSignature(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new SignatureNotFoundException(id));
    }

    private SignatureResponse toResponse(Signature signature) {
        return new SignatureResponse(
                signature.getId(),
                signature.getReportId(),
                signature.getCertificateHolder(),
                signature.getSerialNumber(),
                signature.getCertifyingAuthority(),
                signature.getSignatureAlgorithm(),
                signature.getCertificatePublicKey(),
                signature.getSignature(),
                signature.getSignedAt(),
                signature.getTimestampToken()
        );
    }
}
