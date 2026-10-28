package school.sptech.familia_connect.application.port.out;

public interface FamiliaPortOut {
    int updateIdCarteirinhaById(String idCarteirinha, Integer id);
    boolean existsById(Integer idFamilia);
    int countMembrosByIdFamilia(Integer idFamilia);
}
