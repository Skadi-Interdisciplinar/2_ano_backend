package skadi.api.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import skadi.api.enums.NivelAcesso;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void getAuthorities_returnsRoleFromAccessLevel() {
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN, 1, null);

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
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senhaPura", NivelAcesso.OPERADOR, 1, 2);

        assertEquals("senhaPura", user.getPassword());
        assertEquals("enzo", user.getUsername());
        assertEquals(2, user.getCodGestor());
    }
}
