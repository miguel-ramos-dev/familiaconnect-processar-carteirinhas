package school.sptech.familia_connect.infraestructure.exception;

import lombok.Getter;

@Getter
public class GlobalModelException extends RuntimeException {
    private String message = "Erro no fluxo de processamento de carteirinhas";
    private ErrorEnum status = ErrorEnum.ERRO;

    public GlobalModelException() {

    }

    public GlobalModelException(String cause) {
        super(cause);
        this.message = cause;
    }

}
