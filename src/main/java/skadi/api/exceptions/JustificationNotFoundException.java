package skadi.api.exceptions;

public class JustificationNotFoundException extends RuntimeException {

    public JustificationNotFoundException(Integer alertHandlingId) {
        super("Justificativa não encontrada para o atendimento: " + alertHandlingId);
    }
}
