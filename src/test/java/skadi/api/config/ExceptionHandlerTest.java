package skadi.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import skadi.api.dto.ErrorResponse;
import skadi.api.exceptions.UserAlreadyExistsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleUserAlreadyExists_returns409() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserAlreadyExists(
                new UserAlreadyExistsException()
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().statusCode());
        assertEquals("Nome de usuário já utilizado", response.getBody().message());
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(
                new AccessDeniedException("ADMIN não pode cadastrar SUPER_ADMIN")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().statusCode());
        assertEquals("ADMIN não pode cadastrar SUPER_ADMIN", response.getBody().message());
    }

    @Test
    void handleBadCredentials_returns401() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(
                new BadCredentialsException("Credenciais inválidas")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Credenciais inválidas", response.getBody().message());
    }

    @Test
    void handleUsernameNotFound_returns401() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUsernameNotFound(
                new UsernameNotFoundException("Usuario não encontrado")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Usuario não encontrado", response.getBody().message());
    }

    @Test
    void handleRuntime_returns500() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntime(
                new RuntimeException("Erro inesperado: java.lang.IllegalStateException: falha")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().statusCode());
        assertEquals("Erro inesperado: java.lang.IllegalStateException: falha", response.getBody().message());
    }
}
