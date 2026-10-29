package skadi.api.exceptions;

public class AccessLevelChangeNotAllowedException extends RuntimeException {

    public AccessLevelChangeNotAllowedException() {
        super("Não é permitido alterar o nível de acesso");
    }
}
