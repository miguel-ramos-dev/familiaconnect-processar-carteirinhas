package school.sptech.familia_connect.infraestructure.exception;

import java.util.Map;

public class CamposInvalidosException extends RuntimeException {

    private final Map<String, String> erros;

    public CamposInvalidosException(String mensagem, Map<String, String> erros) {
        super(mensagem);
        this.erros = erros;
    }

    public Map<String, String> getErros() {
        return erros;
    }
}
