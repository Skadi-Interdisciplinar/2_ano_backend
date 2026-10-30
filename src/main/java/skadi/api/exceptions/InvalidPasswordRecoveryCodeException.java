package skadi.api.exceptions;

public class InvalidPasswordRecoveryCodeException extends RuntimeException {

    public InvalidPasswordRecoveryCodeException() {
        super("Código de recuperação inválido ou expirado");
    }
}
