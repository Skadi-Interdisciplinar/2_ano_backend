package skadi.api.exceptions;

public class SignatureAlreadyExistsException extends RuntimeException {

    public SignatureAlreadyExistsException(Integer reportId) {
        super("O relatório " + reportId + " já possui uma assinatura.");
    }
}
