package skadi.api.exceptions;

public class CategoryInvalidTemperatureRangeException extends RuntimeException {

    public CategoryInvalidTemperatureRangeException() {
        super("A temperatura mínima deve ser menor que a temperatura máxima");
    }
}
