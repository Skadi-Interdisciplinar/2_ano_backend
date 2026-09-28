package skadi.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityCongifurationTest {

    @Test
    void encoder_returnsBCryptPasswordEncoder() {
        PasswordEncoder encoder = new SecurityCongifuration().encoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        assertTrue(encoder.matches("senha", encoder.encode("senha")));
    }
}
