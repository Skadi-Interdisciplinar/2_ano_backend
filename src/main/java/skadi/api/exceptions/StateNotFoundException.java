package skadi.api.exceptions;

public class StateNotFoundException extends RuntimeException {

    public StateNotFoundException(Long id) {
        super("Estado não encontrado com o id " + id);
    }
}
