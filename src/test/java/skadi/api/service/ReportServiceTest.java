package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.ReportRequest;
import skadi.api.dto.ReportPatchRequest;
import skadi.api.dto.ReportResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.enums.ReportStatus;
import skadi.api.exceptions.ReportNotFoundException;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.model.Report;
import skadi.api.model.User;
import skadi.api.model.DistributionCenter;
import skadi.api.repository.ReportRepository;
import skadi.api.repository.SignatureRepository;
import skadi.api.repository.AdminDistributionCenterRepository;
import org.springframework.data.domain.Sort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Integer REPORT_ID = 12;
    private static final Integer AUTHOR_ID = 7;
    private static final Integer CD_ID = 1;
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 9, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 9, 30);

    @Mock private ReportRepository reportRepository;
    @Mock private SignatureRepository signatureRepository;
    @Mock private AuditUserContext auditUserContext;
    @Mock private DistributionCenterAccessService accessService;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(reportRepository, signatureRepository, auditUserContext, accessService);
    }

    @Test
    void create_savesReportWithGeneratedHashAuthorAndStatus() throws Exception {
        ReportRequest request = request("conteúdo do relatório", PERIOD_START, PERIOD_END);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report saved = invocation.getArgument(0);
            saved.setId(REPORT_ID);
            return saved;
        });

        ReportResponse response = service.create(request, adminUser());

        verifyAuditUser(AUTHOR_ID);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertEquals(AUTHOR_ID, saved.getGeneratedByUserId());
        assertEquals("conteúdo do relatório", saved.getContent());
        assertEquals(sha256("conteúdo do relatório"), saved.getContentHash());
        assertEquals(PERIOD_START, saved.getPeriodStart());
        assertEquals(PERIOD_END, saved.getPeriodEnd());
        assertEquals(ReportStatus.GERADO, saved.getStatus());
        assertEquals(REPORT_ID, response.id());
        assertEquals(saved.getGeneratedAt(), response.generatedAt());
    }

    @Test
    void create_rejectsPeriodStartingAfterItEnds() {
        ReportRequest request = request("conteúdo", PERIOD_END, PERIOD_START);

        assertThrows(ReportRuleViolationException.class,
                () -> service.create(request, adminUser()));

        verifyNoInteractions(reportRepository, signatureRepository);
    }

    @Test
    void findAll_returnsReportsMappedInRepositoryOrder() {
        Report first = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        Report second = report(13, 8, ReportStatus.ASSINADO);
        when(accessService.accessibleDistributionCenterIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(1L));
        when(reportRepository.findAllByDistributionCenterIdIn(List.of(CD_ID), Sort.by(Sort.Direction.DESC, "generatedAt")))
                .thenReturn(List.of(first, second));

        List<ReportResponse> result = service.findAll(user(NivelAcesso.GESTOR));

        assertEquals(2, result.size());
        assertResponse(result.get(0), first);
        assertResponse(result.get(1), second);
    }

    @Test
    void findAll_returnsEmptyListWhenThereAreNoReports() {
        when(accessService.accessibleDistributionCenterIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(1L));
        when(reportRepository.findAllByDistributionCenterIdIn(List.of(CD_ID), Sort.by(Sort.Direction.DESC, "generatedAt")))
                .thenReturn(List.of());

        assertEquals(List.of(), service.findAll(adminUser()));
    }

    @Test
    void findById_returnsMappedReport() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        when(accessService.accessibleDistributionCenterIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(1L));
        when(reportRepository.findByIdAndDistributionCenterIdIn(REPORT_ID, List.of(CD_ID))).thenReturn(Optional.of(report));

        ReportResponse result = service.findById(REPORT_ID, user(NivelAcesso.GESTOR));

        assertResponse(result, report);
    }

    @Test
    void findById_throwsWhenReportDoesNotExist() {
        when(accessService.accessibleDistributionCenterIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(1L));
        when(reportRepository.findByIdAndDistributionCenterIdIn(REPORT_ID, List.of(CD_ID))).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class,
                () -> service.findById(REPORT_ID, adminUser()));
    }

    @Test
    void findAll_rejectsUsersWithoutReportAccess() {
        assertThrows(AccessDeniedException.class, () -> service.findAll(user(NivelAcesso.OPERADOR)));
        User noCd = new User(); noCd.setId(9L); noCd.setNivelAcesso(NivelAcesso.GESTOR);
        assertThrows(AccessDeniedException.class, () -> service.findAll(noCd));
        verifyNoInteractions(reportRepository);
    }

    @Test
    void findById_allowsSuperAdminToReadAnyReport() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(report));
        assertResponse(service.findById(REPORT_ID, user(NivelAcesso.SUPER_ADMIN)), report);
    }

    @Test
    void update_changesContentHashAndPeriodForAuthorOfGeneratedReport() throws Exception {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        ReportPatchRequest request = new ReportPatchRequest("conteúdo atualizado", PERIOD_START.plusDays(1), PERIOD_END);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        ReportResponse result = service.update(REPORT_ID, request, adminUser());

        verifyAuditUser(AUTHOR_ID);
        assertEquals("conteúdo atualizado", report.getContent());
        assertEquals(sha256("conteúdo atualizado"), report.getContentHash());
        assertEquals(PERIOD_START.plusDays(1), report.getPeriodStart());
        assertEquals(PERIOD_END, report.getPeriodEnd());
        assertEquals(ReportStatus.GERADO, report.getStatus());
        assertResponse(result, report);
    }

    @Test
    void update_changesOnlyProvidedContentAndPreservesOtherFields() throws Exception {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        report.setContent("conteúdo original");
        report.setContentHash(sha256("conteúdo original"));
        report.setPeriodStart(PERIOD_START);
        report.setPeriodEnd(PERIOD_END);
        ReportPatchRequest request = new ReportPatchRequest("conteúdo novo", null, null);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        ReportResponse result = service.update(REPORT_ID, request, adminUser());

        assertEquals("conteúdo novo", report.getContent());
        assertEquals(sha256("conteúdo novo"), report.getContentHash());
        assertEquals(PERIOD_START, report.getPeriodStart());
        assertEquals(PERIOD_END, report.getPeriodEnd());
        assertResponse(result, report);
    }

    @Test
    void update_rejectsEmptyPatch() {
        assertThrows(ReportRuleViolationException.class,
                () -> service.update(REPORT_ID, new ReportPatchRequest(null, null, null), adminUser()));

        verifyNoInteractions(reportRepository, signatureRepository);
    }

    @Test
    void update_rejectsInvalidPeriodAfterMergingWithExistingPeriod() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        report.setPeriodStart(PERIOD_START);
        report.setPeriodEnd(PERIOD_END);
        ReportPatchRequest request = new ReportPatchRequest(null, PERIOD_END.plusDays(1), null);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));

        assertThrows(ReportRuleViolationException.class,
                () -> service.update(REPORT_ID, request, adminUser()));

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void update_throwsNotFoundWhenReportIsNotOwnedByUser() {
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class,
                () -> service.update(REPORT_ID, new ReportPatchRequest("novo", null, null), adminUser()));
    }

    @Test
    void update_rejectsReportThatIsNotGenerated() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.ASSINADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));

        assertThrows(ReportRuleViolationException.class,
                () -> service.update(REPORT_ID, new ReportPatchRequest("novo", null, null), adminUser()));

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void delete_removesUnsignedGeneratedReportForAuthor() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));
        when(signatureRepository.existsByReportId(REPORT_ID)).thenReturn(false);

        service.delete(REPORT_ID, adminUser());

        verifyAuditUser(AUTHOR_ID);
        verify(signatureRepository).existsByReportId(REPORT_ID);
        verify(reportRepository).delete(report);
    }

    @Test
    void delete_rejectsReportThatAlreadyHasSignature() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));
        when(signatureRepository.existsByReportId(REPORT_ID)).thenReturn(true);

        assertThrows(ReportRuleViolationException.class,
                () -> service.delete(REPORT_ID, adminUser()));

        verify(reportRepository, never()).delete(any(Report.class));
    }

    @Test
    void delete_rejectsReportThatIsNotGenerated() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.ARQUIVADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));

        assertThrows(ReportRuleViolationException.class,
                () -> service.delete(REPORT_ID, adminUser()));

        verify(signatureRepository, never()).existsByReportId(REPORT_ID);
        verify(reportRepository, never()).delete(any(Report.class));
    }

    @Test
    void delete_throwsNotFoundWhenReportIsNotOwnedByUser() {
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class,
                () -> service.delete(REPORT_ID, adminUser()));
    }

    @Test
    void updateStatus_archivesSignedReport() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.ASSINADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        ReportResponse result = service.updateStatus(REPORT_ID, ReportStatus.ARQUIVADO, adminUser());

        verifyAuditUser(AUTHOR_ID);
        assertEquals(ReportStatus.ARQUIVADO, report.getStatus());
        assertEquals(ReportStatus.ARQUIVADO, result.status());
    }

    @Test
    void updateStatus_unarchivesReportBackToSigned() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.ARQUIVADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        ReportResponse result = service.updateStatus(REPORT_ID, ReportStatus.ASSINADO, adminUser());

        assertEquals(ReportStatus.ASSINADO, report.getStatus());
        assertEquals(ReportStatus.ASSINADO, result.status());
    }

    @Test
    void updateStatus_rejectsChangingGeneratedReportDirectlyToSignedOrArchived() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));

        assertThrows(ReportRuleViolationException.class,
                () -> service.updateStatus(REPORT_ID, ReportStatus.ASSINADO, adminUser()));
        assertThrows(ReportRuleViolationException.class,
                () -> service.updateStatus(REPORT_ID, ReportStatus.ARQUIVADO, adminUser()));

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void updateStatus_rejectsArchivingReportThatIsNotSigned() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.ARQUIVADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));

        assertThrows(ReportRuleViolationException.class,
                () -> service.updateStatus(REPORT_ID, ReportStatus.ARQUIVADO, adminUser()));

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void updateStatus_rejectsNullStatus() {
        Report report = report(REPORT_ID, AUTHOR_ID, ReportStatus.GERADO);
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.of(report));

        assertThrows(ReportRuleViolationException.class,
                () -> service.updateStatus(REPORT_ID, null, adminUser()));
    }

    @Test
    void updateStatus_throwsNotFoundWhenReportIsNotOwnedByUser() {
        when(reportRepository.findByIdAndGeneratedByUserId(REPORT_ID, AUTHOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class,
                () -> service.updateStatus(REPORT_ID, ReportStatus.ARQUIVADO, adminUser()));
    }

    private ReportRequest request(String content, LocalDate start, LocalDate end) {
        return new ReportRequest(content, start, end, CD_ID);
    }

    private Report report(Integer id, Integer authorId, ReportStatus status) {
        Report report = new Report();
        report.setId(id);
        report.setGeneratedByUserId(authorId);
        report.setDistributionCenterId(CD_ID);
        report.setGeneratedAt(LocalDateTime.of(2026, 10, 10, 12, 0));
        report.setContent("conteúdo de teste");
        report.setContentHash(sha256("conteúdo de teste"));
        report.setPeriodStart(PERIOD_START);
        report.setPeriodEnd(PERIOD_END);
        report.setStatus(status);
        return report;
    }

    private String sha256(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void assertResponse(ReportResponse response, Report report) {
        assertEquals(report.getId(), response.id());
        assertEquals(report.getGeneratedByUserId(), response.generatedByUserId());
        assertEquals(report.getDistributionCenterId(), response.distributionCenterId());
        assertEquals(report.getGeneratedAt(), response.generatedAt());
        assertEquals(report.getContentHash(), response.contentHash());
        assertEquals(report.getPeriodStart(), response.periodStart());
        assertEquals(report.getPeriodEnd(), response.periodEnd());
        assertEquals(report.getStatus(), response.status());
        assertEquals(report.getContent(), response.content());
    }

    private void verifyAuditUser(Integer userId) {
        verify(auditUserContext).setCurrentUser(adminUser());
    }

    private User user(NivelAcesso role) {
        User user = new User();
        user.setId(role == NivelAcesso.ADMIN ? AUTHOR_ID.longValue() : 9L);
        user.setNivelAcesso(role);
        if (role == NivelAcesso.GESTOR || role == NivelAcesso.OPERADOR) {
            user.setCd(List.of(new DistributionCenter(CD_ID.longValue(), "CD de teste", "12345678901234", null)));
        }
        return user;
    }

    private User adminUser() { return user(NivelAcesso.ADMIN); }
}
