package skadi.api.exceptions;

public class BatchStorageRuleViolationException extends RuntimeException {

    public BatchStorageRuleViolationException(String message) {
        super(message);
    }

    public BatchStorageRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
