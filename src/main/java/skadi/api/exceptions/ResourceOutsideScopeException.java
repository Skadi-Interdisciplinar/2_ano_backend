package skadi.api.exceptions;

public class ResourceOutsideScopeException extends RuntimeException {

    public ResourceOutsideScopeException() {
        super("Recurso não encontrado");
    }
}
