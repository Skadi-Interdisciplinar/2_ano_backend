package skadi.api.exceptions;

public class BatchInvalidDatesException extends RuntimeException {

    public BatchInvalidDatesException() {
        super("A data de validade não pode ser anterior à data de fabricação");
    }
}
