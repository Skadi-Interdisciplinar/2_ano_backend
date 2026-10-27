package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.repository.AdminDistributionCenterRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributionCenterAccessServiceTest {

    @Mock private AdminDistributionCenterRepository repository;
    private DistributionCenterAccessService service;

    @BeforeEach
    void setUp() {
        service = new DistributionCenterAccessService(repository);
    }

    @Test
    void superAdminCanReadAnyDistributionCenterButCannotWriteOperationalData() {
        User superAdmin = user(1L, NivelAcesso.SUPER_ADMIN, null);

        assertTrue(service.canRead(superAdmin, 99L));
        assertThrows(AccessDeniedException.class,
                () -> service.requireOperationalWrite(superAdmin, 99L));
    }

    @Test
    void adminCanOnlyReadAndOwnLinkedDistributionCenters() {
        User admin = user(2L, NivelAcesso.ADMIN, null);
        when(repository.existsByAdmin_IdAndDistributionCenter_Id(2L, 10L)).thenReturn(true);
        when(repository.existsByAdmin_IdAndDistributionCenter_Id(2L, 11L)).thenReturn(false);

        assertTrue(service.canRead(admin, 10L));
        assertFalse(service.canRead(admin, 11L));
        service.requireAdminOwnership(admin, 10L);
        assertThrows(AccessDeniedException.class,
                () -> service.requireAdminOwnership(admin, 11L));
    }

    @Test
    void managerAndOperatorAreRestrictedToTheirDirectDistributionCenter() {
        User manager = user(3L, NivelAcesso.GESTOR, 20L);
        User operator = user(4L, NivelAcesso.OPERADOR, 20L);

        assertTrue(service.canRead(manager, 20L));
        assertTrue(service.canRead(operator, 20L));
        assertFalse(service.canRead(manager, 21L));
        assertFalse(service.canRead(operator, 21L));
        assertEquals(List.of(20L), service.accessibleDistributionCenterIds(operator));
    }

    private User user(Long id, NivelAcesso level, Long distributionCenterId) {
        DistributionCenter distributionCenter = distributionCenterId == null ? null
                : new DistributionCenter(distributionCenterId, "CD", "12345678901234", null);
        return new User(id, "Usuário", "usuario" + id, "12345678901",
                "usuario" + id + "@teste.com", "senha", level, distributionCenter, null, null);
    }
}
