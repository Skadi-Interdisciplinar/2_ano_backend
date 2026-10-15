package skadi.api.exceptions;

public class ThermometerNotFoundException extends RuntimeException {

    public ThermometerNotFoundException(Long id) {
        super("Termômetro não encontrado com o id " + id);
    }
}
