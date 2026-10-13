package skadi.api.exceptions;

public class ColdRoomNotFoundException extends RuntimeException {
    public ColdRoomNotFoundException(Long coldRoomId) {
        super("Câmara frigorífica não encontrada: " + coldRoomId);
    }
}
