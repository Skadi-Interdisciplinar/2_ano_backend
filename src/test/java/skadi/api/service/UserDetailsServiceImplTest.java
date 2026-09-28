package skadi.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.ErrorResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_returnsUserWhenFound() {
        User user = new User("Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.GESTOR, 1);
        when(userRepository.findByUsername("enzo")).thenReturn(Optional.of(user));

        UserDetails encontrado = userDetailsService.loadUserByUsername("enzo");

        assertEquals("enzo", encontrado.getUsername());
        assertEquals("senha", encontrado.getPassword());
    }

    @Test
    void loadUserByUsername_throwsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        UsernameNotFoundException erro = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("fantasma")
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUsernameNotFound(erro);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Usuario não encontrado", response.getBody().message());
    }
}
