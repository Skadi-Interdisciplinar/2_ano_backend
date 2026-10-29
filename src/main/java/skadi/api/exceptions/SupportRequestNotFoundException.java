package skadi.api.exceptions;

public class SupportRequestNotFoundException extends RuntimeException {

    public SupportRequestNotFoundException() {
        super("Chamado não encontrado");
    }
}
