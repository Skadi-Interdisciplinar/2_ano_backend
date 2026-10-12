package skadi.api.exceptions;

public class TemperatureReadingNotFoundException extends RuntimeException {
    public TemperatureReadingNotFoundException(Long coldRoomId) {
        super("Temperatura atual não encontrada para a câmara frigorífica " + coldRoomId);
    }
}
