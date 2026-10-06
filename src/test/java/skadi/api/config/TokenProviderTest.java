package skadi.api.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    private static final String SECRET = "skadi-test-jwt-secret-key-32chars-min";

    private TokenProvider tokenProvider;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "key", SECRET);
        ReflectionTestUtils.setField(tokenProvider, "expirationTime", 3600L);
    }

    @Test
    void buildToken_generatesJwtWithUsernameAsSubject() {
        String token = tokenProvider.buildToken("enzo");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals("enzo", tokenProvider.getUsername(token));
    }

    @Test
    void generateToken_usesPrincipalUsername() {
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.OPERARIO, 1, 2);
        when(authentication.getPrincipal()).thenReturn(user);

        String token = tokenProvider.generateToken(authentication);

        assertEquals("enzo", tokenProvider.getUsername(token));
        assertTrue(tokenProvider.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsTrueForGeneratedToken() {
        String token = tokenProvider.buildToken("enzo");

        assertTrue(tokenProvider.isTokenValid(token));
    }

    @Test
    void isTokenValid_returnsFalseForInvalidToken() {
        assertFalse(tokenProvider.isTokenValid("token-invalido"));
    }

    @Test
    void isTokenValid_returnsFalseWhenSignedWithAnotherKey() {
        String token = tokenProvider.buildToken("enzo");
        TokenProvider outroProvider = new TokenProvider();
        ReflectionTestUtils.setField(outroProvider, "key", "outra-chave-jwt-secret-32chars-min");
        ReflectionTestUtils.setField(outroProvider, "expirationTime", 3600L);

        assertFalse(outroProvider.isTokenValid(token));
    }

    @Test
    void generateToken_usesAuthenticationNameWhenPrincipalIsNotUserDetails() {
        when(authentication.getPrincipal()).thenReturn("enzo");
        when(authentication.getName()).thenReturn("enzo");

        String token = tokenProvider.generateToken(authentication);

        assertEquals("enzo", tokenProvider.getUsername(token));
        assertTrue(tokenProvider.isTokenValid(token));
    }

    @Test
    void isTokenValid_throwsExpiredJwtExceptionWhenTokenIsExpired() {
        ReflectionTestUtils.setField(tokenProvider, "expirationTime", -10L);
        String token = tokenProvider.buildToken("enzo");

        ExpiredJwtException erro = assertThrows(
                ExpiredJwtException.class,
                () -> tokenProvider.isTokenValid(token)
        );

        assertEquals("Token inválido", erro.getMessage());
    }
}
