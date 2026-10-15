package skadi.api.exceptions;

public class BatchNotFoundException extends RuntimeException {

    public BatchNotFoundException(Long id) {
        super("Lote não encontrado com o id " + id);
    }
}
