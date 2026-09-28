package skadi.api.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTConfigurationFilterTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JWTConfigurationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTConfigurationFilter(tokenProvider, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingHeader_doesNotAuthenticateAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void headerWithoutBearer_doesNotAuthenticateAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidBearer_doesNotAuthenticateAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-ruim");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenProvider.isTokenValid("token-ruim")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validBearer_setsSecurityContext() throws Exception {
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN, 1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.isTokenValid("jwt-valido")).thenReturn(true);
        when(tokenProvider.getUsername("jwt-valido")).thenReturn("enzo");
        when(userDetailsService.loadUserByUsername("enzo")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        assertEquals("enzo", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(1, SecurityContextHolder.getContext().getAuthentication().getAuthorities().size());
        verify(filterChain).doFilter(request, response);
    }
}
