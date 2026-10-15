package skadi.api.exceptions;

public class DuplicatedCnpjException extends RuntimeException {
    public DuplicatedCnpjException(String message) {
        super(message);
    }

    public DuplicatedCnpjException() {
        super("Cnpj já existente no banco de dados, favor inserir o correto");
    }
}
