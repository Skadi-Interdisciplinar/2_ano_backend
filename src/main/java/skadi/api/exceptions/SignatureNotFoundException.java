package skadi.api.exceptions;

public class SignatureNotFoundException extends RuntimeException {

    public SignatureNotFoundException(Integer id) {
        super("Assinatura não encontrada: " + id);
    }
}
