package school.sptech.familia_connect.infraestructure.exception;

public enum ErrorEnum {
    SUCESSO("SUCESSO"),
    ERRO("ERRO");

    private String desc;

    ErrorEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
