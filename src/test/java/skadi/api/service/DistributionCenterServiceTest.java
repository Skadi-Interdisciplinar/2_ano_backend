package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.AdminDistributionCenter;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.repository.AdminDistributionCenterRepository;
import skadi.api.repository.DistributionCenterRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributionCenterServiceTest {

    @Mock private DistributionCenterRepository repository;
    @Mock private AdminDistributionCenterRepository linkRepository;
    @Mock private DistributionCenterAccessService accessService;
    @Mock private AuditUserContext auditUserContext;
    private DistributionCenterService service;

    @BeforeEach
    void setUp() {
        service = new DistributionCenterService(
                repository, linkRepository, accessService, auditUserContext);
    }

    @Test
    void createLinksTheNewDistributionCenterToTheAuthenticatedAdmin() {
        User admin = new User(7L, "Admin", "admin", "12345678901", "admin@teste.com",
                "senha", NivelAcesso.ADMIN, null, null, null);
        DistributionCenterRequest request = new DistributionCenterRequest(
                "CD Principal", "12345678901234");
        when(repository.existsByCnpj(request.cnpj())).thenReturn(false);
        when(repository.save(any(DistributionCenter.class))).thenAnswer(invocation -> {
            DistributionCenter saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        service.insertCd(request, admin);

        verify(auditUserContext).setCurrentUser(7L);
        ArgumentCaptor<AdminDistributionCenter> linkCaptor =
                ArgumentCaptor.forClass(AdminDistributionCenter.class);
        verify(linkRepository).save(linkCaptor.capture());
        assertEquals(admin, linkCaptor.getValue().getAdmin());
        assertEquals(15L, linkCaptor.getValue().getDistributionCenter().getId());
    }
}
