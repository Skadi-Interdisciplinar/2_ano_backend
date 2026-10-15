package skadi.api.exceptions;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(Integer id) {
        super("Relatório não encontrado: " + id);
    }
}
