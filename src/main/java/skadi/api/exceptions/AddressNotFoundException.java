package skadi.api.exceptions;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(Long id) {
        super("Endereço não encontrado com o id " + id);
    }
}
