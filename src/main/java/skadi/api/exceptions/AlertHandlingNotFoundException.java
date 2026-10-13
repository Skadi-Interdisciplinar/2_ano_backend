package skadi.api.exceptions;

public class AlertHandlingNotFoundException extends RuntimeException {
    private AlertHandlingNotFoundException(String message) {
        super(message);
    }

    public static AlertHandlingNotFoundException forAlertId(Integer alertId) {
        return new AlertHandlingNotFoundException(
                "Atendimento não encontrado para o alerta: " + alertId);
    }

    public static AlertHandlingNotFoundException forHandlingId(Integer handlingId) {
        return new AlertHandlingNotFoundException(
                "Atendimento não encontrado: " + handlingId);
    }
}
