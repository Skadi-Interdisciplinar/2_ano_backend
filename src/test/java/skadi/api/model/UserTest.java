package skadi.api.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.GestorAssociationRuleException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void getAuthorities_returnsRoleFromAccessLevel() {
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                List.of(new DistributionCenter(1L, "CD Osasco", "12345678901234", null)), null);

        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(List.of("ROLE_ADMIN"), authorities);
    }

    @Test
    void getAuthorities_returnsEmptyListWhenAccessLevelIsNull() {
        User user = new User();

        assertTrue(user.getAuthorities().isEmpty());
    }

    @Test
    void getPassword_returnsPlainPassword() {
        User gestor = gestor(2L);
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senhaPura", NivelAcesso.OPERADOR,
                new DistributionCenter(1L, "CD Osasco", "12345678901234", null), gestor);

        assertEquals("senhaPura", user.getPassword());
        assertEquals("enzo", user.getUsername());
        assertEquals(gestor, user.getGestor());
    }

    @Test
    void adminConstructor_setsCdListAndLeavesUniqueCdNull() {
        DistributionCenter osasco = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User gestor = gestor(10L);
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                List.of(osasco), gestor);

        assertNull(user.getCdGestor());
        assertEquals(List.of(osasco), user.getCd());
        assertEquals(gestor, user.getGestor());
        assertEquals(1, user.getCdId());
    }

    @Test
    void operatorConstructor_setsUniqueCdAndLeavesListNull() {
        DistributionCenter osasco = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User gestor = gestor(2L);
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.OPERADOR,
                osasco, gestor);

        assertEquals(osasco, user.getCdGestor());
        assertNull(user.getCd());
        assertEquals(gestor, user.getGestor());
        assertEquals(1, user.getCdId());
    }

    @Test
    void validateGestorAssociation_rejectsUserAsOwnManager() {
        User user = new User();
        user.setId(4L);
        user.setGestor(user);

        GestorAssociationRuleException exception = assertThrows(
                GestorAssociationRuleException.class,
                user::validateGestorAssociation
        );

        assertEquals("O usuário não pode ser o próprio gestor.", exception.getMessage());
    }

    @Test
    void validateGestorAssociation_rejectsManagerFromAnotherDistributionCenter() {
        DistributionCenter osasco = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        DistributionCenter campinas = new DistributionCenter(2L, "CD Campinas", "22345678901234", null);
        User gestor = gestor(8L);
        gestor.setCdGestor(campinas);
        User user = new User();
        user.setId(4L);
        user.setCdGestor(osasco);
        user.setGestor(gestor);

        GestorAssociationRuleException exception = assertThrows(
                GestorAssociationRuleException.class,
                user::validateGestorAssociation
        );

        assertEquals(
                "O gestor responsável deve pertencer ao mesmo centro de distribuição.",
                exception.getMessage()
        );
    }

    @Test
    void validateGestorAssociation_acceptsManagerFromTheSameDistributionCenter() {
        DistributionCenter osasco = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User gestor = gestor(8L);
        gestor.setCdGestor(osasco);
        User user = new User();
        user.setId(4L);
        user.setCdGestor(osasco);
        user.setGestor(gestor);

        user.validateGestorAssociation();

        assertEquals(gestor, user.getGestor());
    }

    private User gestor(Long id) {
        User gestor = new User();
        gestor.setId(id);
        return gestor;
    }
}
