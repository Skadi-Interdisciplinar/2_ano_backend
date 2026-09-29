package skadi.api.exceptions;

public class UserAlreadyExistsException extends  RuntimeException {

    public UserAlreadyExistsException() {
        super("Nome de usuário já utilizado");
    }

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
