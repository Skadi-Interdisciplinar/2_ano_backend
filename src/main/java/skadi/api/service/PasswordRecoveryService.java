package skadi.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.exceptions.InvalidPasswordRecoveryCodeException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordRecoveryService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "skadi:senha:recuperacao:";
    private static final String VERIFIED_PREFIX = "skadi:senha:recuperacao:verificado:";
    private static final String VERIFIED = "ok";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final String from;

    public PasswordRecoveryService(
            UserRepository repository,
            StringRedisTemplate redisTemplate,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            @Value("${spring.mail.username}") String from
    ) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.from = from;
    }

    public void recoverPassword(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("E-mail não encontrado"));
        String code = generateCode();
        redisTemplate.delete(verifiedKey(email));
        redisTemplate.opsForValue().set(key(email), code, CODE_TTL);
        sendCode(user.getEmail(), code);
    }

    public void verifyCode(String email, String codigo) {
        requireUser(email);
        requireValidCode(email, codigo);
        redisTemplate.opsForValue().set(verifiedKey(email), VERIFIED, remainingTtl(email));
        redisTemplate.delete(key(email));
    }

    @Transactional
    public void resetPassword(String email, String senha) {
        User user = requireUser(email);
        String verified = redisTemplate.opsForValue().get(verifiedKey(email));
        if (!VERIFIED.equals(verified)) {
            throw new InvalidPasswordRecoveryCodeException();
        }
        user.setSenha(passwordEncoder.encode(senha));
        repository.save(user);
        redisTemplate.delete(verifiedKey(email));
    }

    private User requireUser(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("E-mail não encontrado"));
    }

    private void requireValidCode(String email, String codigo) {
        String stored = redisTemplate.opsForValue().get(key(email));
        if (stored == null || !stored.equals(codigo)) {
            throw new InvalidPasswordRecoveryCodeException();
        }
    }

    private void sendCode(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Código de verificação");
            helper.setText(htmlWithCode(code), true);
            helper.addInline("logo", new ClassPathResource("mail/logo.png"));
            mailSender.send(message);
        } catch (MessagingException | IOException | MailException exception ) {
            throw new IllegalStateException(exception.getCause());
        }
    }

    private String htmlWithCode(String code) throws IOException {
        ClassPathResource template = new ClassPathResource("mail/password-recovery.html");
        String html = new String(template.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return html.replace("{{codigo}}", code);
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private Duration remainingTtl(String email) {
        Long seconds = redisTemplate.getExpire(key(email), TimeUnit.SECONDS);
        if (seconds == null || seconds <= 0) {
            throw new InvalidPasswordRecoveryCodeException();
        }
        return Duration.ofSeconds(seconds);
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }

    private String verifiedKey(String email) {
        return VERIFIED_PREFIX + email;
    }
}
