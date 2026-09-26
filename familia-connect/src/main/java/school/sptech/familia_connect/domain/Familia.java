package school.sptech.familia_connect.domain;

public class Familia {

    private Integer id;
    private String nomeResponsavel;
    private String cpfResponsavel;
    private String telefone;

    public Familia() {
    }

    public Familia(Integer id, String nomeResponsavel, String cpfResponsavel, String telefone) {
        this.id = id;
        this.nomeResponsavel = nomeResponsavel;
        this.cpfResponsavel = cpfResponsavel;
        this.telefone = telefone;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public String getCpfResponsavel() {
        return cpfResponsavel;
    }

    public void setCpfResponsavel(String cpfResponsavel) {
        this.cpfResponsavel = cpfResponsavel;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
