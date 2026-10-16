package skadi.api.exceptions;

public class ColdRoomInvalidTemperatureRangeException extends RuntimeException {

    public ColdRoomInvalidTemperatureRangeException() {
        super("A temperatura mínima deve ser menor que a temperatura máxima");
    }
}
