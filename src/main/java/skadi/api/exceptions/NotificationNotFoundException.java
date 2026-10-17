package skadi.api.exceptions;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Integer id) {
        super("Notificação não encontrada: " + id);
    }
}
