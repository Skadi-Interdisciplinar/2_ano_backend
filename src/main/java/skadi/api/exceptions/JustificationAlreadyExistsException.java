package skadi.api.exceptions;

public class JustificationAlreadyExistsException extends RuntimeException {

    public JustificationAlreadyExistsException(Integer alertHandlingId) {
        super("Já existe uma justificativa para o atendimento: " + alertHandlingId);
    }
}
