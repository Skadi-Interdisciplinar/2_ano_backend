package skadi.api.exceptions;

public class PasswordConfirmationMismatchException extends RuntimeException {

    public PasswordConfirmationMismatchException() {
        super("A confirmação da senha não confere");
    }
}
