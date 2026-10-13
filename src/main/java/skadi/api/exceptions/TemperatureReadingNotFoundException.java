package skadi.api.exceptions;

public class TemperatureReadingNotFoundException extends RuntimeException {
    public TemperatureReadingNotFoundException(Integer readingId) {
        super("Leitura de temperatura não encontrada: " + readingId);
    }
}
