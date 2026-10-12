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
import skadi.api.repository.SignatureRepository;

import java.util.List;
import java.util.Optional;

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

    public Optional<SignatureResponse> findByReportId(Integer reportId, Integer cdId, NivelAcesso accessLevel) {
        reportService.findReportForCd(reportId, cdId, accessLevel);
        return repository.findByReportIdAndAuthorCd(reportId, cdId).map(SignatureMapper::toDTO);
    }

    public List<SignatureResponse> findAllByCd(Integer cdId, NivelAcesso accessLevel) {
        reportService.requireReportReadAccess(cdId, accessLevel);
        return repository.findAllByReportAuthorCdOrderByIdDesc(cdId).stream()
                .map(SignatureMapper::toDTO)
                .toList();
    }

    public SignatureResponse findById(Integer id, Integer cdId, NivelAcesso accessLevel) {
        reportService.requireReportReadAccess(cdId, accessLevel);
        Signature signature = repository.findByIdAndReportAuthorCd(id, cdId)
                .orElseThrow(() -> new SignatureNotFoundException(id));
        return SignatureMapper.toDTO(signature);
    }

}
