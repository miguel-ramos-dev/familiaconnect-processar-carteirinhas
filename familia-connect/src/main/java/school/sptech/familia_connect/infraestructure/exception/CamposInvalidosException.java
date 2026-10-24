package school.sptech.familia_connect.infraestructure.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class CamposInvalidosException extends GlobalModelException {

    private final Map<String, String> erros;

    public CamposInvalidosException(String mensagem, Map<String, String> erros) {
        super(mensagem);
        this.erros = erros;
    }

}
