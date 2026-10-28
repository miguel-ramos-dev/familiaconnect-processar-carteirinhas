package school.sptech.familia_connect.domain;

public class Familia {

    private Integer idFamilia;
    private String nomeResponsavel;
    private String cpfResponsavel;
    private String telefone;
    private Integer quantidadeMembros;

    public Familia() {
    }

    public Familia(Integer idFamilia, String nomeResponsavel, String cpfResponsavel, String telefone) {
        this.idFamilia = idFamilia;
        this.nomeResponsavel = nomeResponsavel;
        this.cpfResponsavel = cpfResponsavel;
        this.telefone = telefone;
    }

    public Integer getIdFamilia() {
        return idFamilia;
    }

    public void setIdFamilia(Integer id) {
        this.idFamilia = id;
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

    public Integer getQuantidadeMembros() {
        return quantidadeMembros;
    }

    public void setQuantidadeMembros(Integer quantidadeMembros) {
        this.quantidadeMembros = quantidadeMembros;
    }
}
