package skadi.api.exceptions;

public class CurrentTemperatureNotFoundException extends RuntimeException {
    public CurrentTemperatureNotFoundException(Long coldRoomId) {
        super("Temperatura atual não encontrada para a câmara frigorífica " + coldRoomId);
    }
}
