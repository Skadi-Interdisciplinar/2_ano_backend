package skadi.api.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import skadi.api.exceptions.InvalidPasswordRecoveryCodeException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    private static final String EMAIL = "enzo@email.com";
    private static final String KEY = "skadi:senha:recuperacao:" + EMAIL;

    @Mock
    private UserRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> values;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(values);
        service = new PasswordRecoveryService(
                repository,
                redisTemplate,
                mailSender,
                passwordEncoder,
                "skadi.enterprise@gmail.com"
        );
    }

    @Test
    void recoverPasswordStoresCodeForTenMinutesAndSendsHtml() throws Exception {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        service.recoverPassword(EMAIL);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq(KEY), codeCaptor.capture(), eq(Duration.ofMinutes(10)));
        String code = codeCaptor.getValue();
        assertTrue(code.matches("\\d{6}"));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        messageCaptor.getValue().writeTo(output);
        String raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains(code));
        assertTrue(raw.contains("cid:logo"));
        assertTrue(raw.contains("10 minutos"));
        assertTrue(raw.contains("skadi.enterprise@gmail.com"));
    }

    @Test
    void recoverPasswordRejectsUnknownEmail() {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> service.recoverPassword(EMAIL));

        assertEquals("E-mail não encontrado", exception.getMessage());
        verify(values, never()).set(any(), any(), any(Duration.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void resetPasswordChangesPasswordWhenCodeMatches() {
        User user = user();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(values.get(KEY)).thenReturn("123456");
        when(passwordEncoder.encode("novaSenha")).thenReturn("hash");

        service.resetPassword(EMAIL, "123456", "novaSenha");

        assertEquals("hash", user.getSenha());
        verify(repository).save(user);
        verify(redisTemplate).delete(KEY);
    }

    @Test
    void resetPasswordRejectsInvalidCode() {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(values.get(KEY)).thenReturn("123456");

        assertThrows(InvalidPasswordRecoveryCodeException.class,
                () -> service.resetPassword(EMAIL, "000000", "novaSenha"));

        verify(repository, never()).save(any());
        verify(redisTemplate, never()).delete(eq(KEY));
    }

    @Test
    void resetPasswordRejectsExpiredCode() {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(values.get(KEY)).thenReturn(null);

        assertThrows(InvalidPasswordRecoveryCodeException.class,
                () -> service.resetPassword(EMAIL, "123456", "novaSenha"));

        verify(repository, never()).save(any());
    }

    private User user() {
        User user = new User();
        user.setId(4L);
        user.setEmail(EMAIL);
        user.setSenha("senha-atual");
        return user;
    }
}
