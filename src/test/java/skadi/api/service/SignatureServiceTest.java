package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.SignatureRequest;
import skadi.api.dto.SignatureResponse;
import skadi.api.enums.ReportStatus;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.exceptions.SignatureAlreadyExistsException;
import skadi.api.exceptions.SignatureNotFoundException;
import skadi.api.model.Report;
import skadi.api.model.Signature;
import skadi.api.repository.SignatureRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    @Mock private ReportService reportService;
    @Mock private SignatureRepository repository;
    @Mock private AuditUserContext auditUserContext;

    private SignatureService service;

    @BeforeEach
    void setUp() {
        service = new SignatureService(
                reportService, repository, new SignatureVerificationService(), auditUserContext);
    }

    @Test
    void create_savesSignatureAndMarksReportAsSigned() {
        Report report = report(ReportStatus.GERADO);
        SignatureRequest request = request();
        when(reportService.findReportForAuthor(4, 7)).thenReturn(report);
        when(repository.save(any(Signature.class))).thenAnswer(invocation -> {
            Signature saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });

        SignatureResponse result = service.create(4, request, 7);

        verify(auditUserContext).setCurrentUser(7);

        ArgumentCaptor<Signature> captor = ArgumentCaptor.forClass(Signature.class);
        verify(repository).save(captor.capture());
        Signature saved = captor.getValue();
        assertEquals(4, saved.getReportId());
        assertEquals(request.certificateHolder(), saved.getCertificateHolder());
        assertEquals(request.serialNumber(), saved.getSerialNumber());
        assertEquals(request.certifyingAuthority(), saved.getCertifyingAuthority());
        assertEquals(request.signatureAlgorithm(), saved.getSignatureAlgorithm());
        assertEquals(request.certificatePublicKey(), saved.getCertificatePublicKey());
        assertEquals(request.signature(), saved.getSignature());
        assertEquals(request.timestampToken(), saved.getTimestampToken());
        assertEquals(LocalDate.now(), saved.getSignedAt());
        assertEquals(ReportStatus.ASSINADO, report.getStatus());
        assertEquals(12, result.id());
        assertEquals(4, result.reportId());
        assertEquals(request.signature(), result.signature());
    }

    @Test
    void create_rejectsReportThatIsNotGenerated() {
        Report report = report(ReportStatus.ASSINADO);
        when(reportService.findReportForAuthor(4, 7)).thenReturn(report);

        assertThrows(ReportRuleViolationException.class,
                () -> service.create(4, request(), 7));

        verify(repository, never()).save(any(Signature.class));
    }

    @Test
    void create_throwsSpecificExceptionWhenReportAlreadyHasSignature() {
        Report report = report(ReportStatus.GERADO);
        when(reportService.findReportForAuthor(4, 7)).thenReturn(report);
        when(repository.existsByReportId(4)).thenReturn(true);

        assertThrows(SignatureAlreadyExistsException.class,
                () -> service.create(4, request(), 7));

        assertEquals(ReportStatus.GERADO, report.getStatus());
        verify(repository, never()).save(any(Signature.class));
    }

    @Test
    void create_rejectsInvalidSignatureWithoutSaving() {
        Report report = report(ReportStatus.GERADO);
        SignatureRequest validRequest = request();
        SignatureRequest invalidRequest = new SignatureRequest(
                validRequest.certificateHolder(),
                validRequest.serialNumber(),
                validRequest.certifyingAuthority(),
                validRequest.signatureAlgorithm(),
                validRequest.certificatePublicKey(),
                Base64.getEncoder().encodeToString("signature-tampered".getBytes(StandardCharsets.UTF_8)),
                validRequest.timestampToken()
        );
        when(reportService.findReportForAuthor(4, 7)).thenReturn(report);

        assertThrows(ReportRuleViolationException.class,
                () -> service.create(4, invalidRequest, 7));

        assertEquals(ReportStatus.GERADO, report.getStatus());
        verify(repository, never()).save(any(Signature.class));
    }

    @Test
    void findByReportId_returnsMappedSignature() {
        Report report = report(ReportStatus.ASSINADO);
        Signature signature = signature(12, 4);
        when(reportService.findReportForCd(4, 1, NivelAcesso.GESTOR)).thenReturn(report);
        when(repository.findByReportIdAndAuthorCd(4, 1)).thenReturn(Optional.of(signature));

        Optional<SignatureResponse> result = service.findByReportId(4, 1, NivelAcesso.GESTOR);

        verify(reportService).findReportForCd(4, 1, NivelAcesso.GESTOR);
        verify(repository).findByReportIdAndAuthorCd(4, 1);
        assertEquals(true, result.isPresent());
        assertResponse(result.orElseThrow(), signature);
    }

    @Test
    void findByReportId_returnsEmptyWhenNoneExists() {
        when(reportService.findReportForCd(4, 1, NivelAcesso.ADMIN)).thenReturn(report(ReportStatus.GERADO));
        when(repository.findByReportIdAndAuthorCd(4, 1)).thenReturn(Optional.empty());

        Optional<SignatureResponse> result = service.findByReportId(4, 1, NivelAcesso.ADMIN);

        assertEquals(Optional.empty(), result);
        verify(reportService).findReportForCd(4, 1, NivelAcesso.ADMIN);
    }

    @Test
    void findAllByCd_returnsSignaturesForUsersCd() {
        Signature signature = signature(12, 4);
        when(repository.findAllByReportAuthorCdOrderByIdDesc(1)).thenReturn(List.of(signature));

        List<SignatureResponse> result = service.findAllByCd(1, NivelAcesso.GESTOR);

        verify(repository).findAllByReportAuthorCdOrderByIdDesc(1);
        assertEquals(1, result.size());
        assertResponse(result.get(0), signature);
    }

    @Test
    void findAllByCd_returnsEmptyListWhenCdHasNoSignatures() {
        when(repository.findAllByReportAuthorCdOrderByIdDesc(1)).thenReturn(List.of());

        assertEquals(List.of(), service.findAllByCd(1, NivelAcesso.ADMIN));
    }

    @Test
    void findById_returnsSignatureForReportAuthorCd() {
        Signature signature = signature(12, 4);
        when(repository.findByIdAndReportAuthorCd(12, 1)).thenReturn(Optional.of(signature));

        SignatureResponse result = service.findById(12, 1, NivelAcesso.GESTOR);

        verify(repository).findByIdAndReportAuthorCd(12, 1);
        assertResponse(result, signature);
    }

    @Test
    void findById_throwsWhenSignatureDoesNotExist() {
        when(repository.findByIdAndReportAuthorCd(99, 1)).thenReturn(Optional.empty());

        assertThrows(SignatureNotFoundException.class,
                () -> service.findById(99, 1, NivelAcesso.ADMIN));
    }

    @Test
    void findAllByCd_rejectsOperatorOrMissingCd() {
        doThrow(new AccessDeniedException("Sem permissão"))
                .when(reportService).requireReportReadAccess(1, NivelAcesso.OPERADOR);
        doThrow(new AccessDeniedException("Sem CD"))
                .when(reportService).requireReportReadAccess(null, NivelAcesso.GESTOR);
        assertThrows(AccessDeniedException.class, () -> service.findAllByCd(1, NivelAcesso.OPERADOR));
        assertThrows(AccessDeniedException.class, () -> service.findAllByCd(null, NivelAcesso.GESTOR));
    }

    @Test
    void findById_rejectsSuperAdmin() {
        doThrow(new AccessDeniedException("Sem permissão"))
                .when(reportService).requireReportReadAccess(1, NivelAcesso.SUPER_ADMIN);
        assertThrows(AccessDeniedException.class,
                () -> service.findById(12, 1, NivelAcesso.SUPER_ADMIN));
    }

    private Report report(ReportStatus status) {
        Report report = new Report();
        report.setId(4);
        report.setStatus(status);
        report.setContentHash("hash-for-tests");
        return report;
    }

    private SignatureRequest request() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
            signer.initSign(keyPair.getPrivate());
            signer.update(SignatureVerificationService.payload(4, "hash-for-tests"));

            return new SignatureRequest(
                    "Assinante de teste",
                    "SERIE-TESTE-1",
                    "Autoridade de teste",
                    "SHA256withRSA",
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                    Base64.getEncoder().encodeToString(signer.sign()),
                    "carimbo-demonstrativo"
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível preparar a assinatura de teste.", exception);
        }
    }

    private Signature signature(Integer id, Integer reportId) {
        Signature signature = new Signature();
        signature.setId(id);
        signature.setReportId(reportId);
        signature.setCertificateHolder("Assinante de teste");
        signature.setSerialNumber("SERIE-TESTE-1");
        signature.setCertifyingAuthority("Autoridade de teste");
        signature.setSignatureAlgorithm("SHA256withRSA");
        signature.setCertificatePublicKey("chave-publica-demonstrativa");
        signature.setSignature("assinatura-demonstrativa");
        signature.setSignedAt(LocalDate.of(2026, 10, 10));
        signature.setTimestampToken("carimbo-demonstrativo");
        return signature;
    }

    private void assertResponse(SignatureResponse response, Signature signature) {
        assertEquals(signature.getId(), response.id());
        assertEquals(signature.getReportId(), response.reportId());
        assertEquals(signature.getCertificateHolder(), response.certificateHolder());
        assertEquals(signature.getSerialNumber(), response.serialNumber());
        assertEquals(signature.getCertifyingAuthority(), response.certifyingAuthority());
        assertEquals(signature.getSignatureAlgorithm(), response.signatureAlgorithm());
        assertEquals(signature.getCertificatePublicKey(), response.certificatePublicKey());
        assertEquals(signature.getSignature(), response.signature());
        assertEquals(signature.getSignedAt(), response.signedAt());
        assertEquals(signature.getTimestampToken(), response.timestampToken());
    }
}
