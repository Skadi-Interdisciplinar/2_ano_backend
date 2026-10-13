package skadi.api.exceptions;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Integer alertId) {
        super("Alerta não encontrado: " + alertId);
    }
}
