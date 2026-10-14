package skadi.api.exceptions;

public class CdNotFoundException extends RuntimeException {
    public CdNotFoundException() {
        super("Centro de distribuição não encontrado");
    }

    public CdNotFoundException(String message) {
        super(message);
    }
}
