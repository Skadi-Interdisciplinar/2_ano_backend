package skadi.api.exceptions;

public class AddressAlreadyExistsException extends RuntimeException {

    public AddressAlreadyExistsException() {
        super("Já existe um endereço com esse CEP");
    }
}
