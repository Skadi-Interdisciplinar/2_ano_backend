package skadi.api.exceptions;

public class BatchStorageNotFoundException extends RuntimeException {

    public BatchStorageNotFoundException(Long storageId, Long batchId) {
        super("Armazenamento " + storageId + " não encontrado para o lote " + batchId);
    }
}
