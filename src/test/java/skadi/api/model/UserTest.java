package skadi.api.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import skadi.api.enums.NivelAcesso;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senhaPura", NivelAcesso.OPERADOR,
                new DistributionCenter(1L, "CD Osasco", "12345678901234", null), 2);

        assertEquals("senhaPura", user.getPassword());
        assertEquals("enzo", user.getUsername());
        assertEquals(2, user.getCodGestor());
    }

    @Test
    void adminConstructor_setsCdListAndLeavesUniqueCdNull() {
        DistributionCenter osasco = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                List.of(osasco), 10);

        assertNull(user.getCdGestor());
        assertEquals(List.of(osasco), user.getCd());
        assertEquals(10, user.getCodGestor());
        assertEquals(1, user.getCdId());
    }

    @Test
    void operatorConstructor_setsUniqueCdAndLeavesListNull() {
        DistributionCenter osasco = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.OPERADOR,
                osasco, 2);

        assertEquals(osasco, user.getCdGestor());
        assertNull(user.getCd());
        assertEquals(2, user.getCodGestor());
        assertEquals(1, user.getCdId());
    }
}
