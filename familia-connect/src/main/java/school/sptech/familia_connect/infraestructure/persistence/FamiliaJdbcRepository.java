package school.sptech.familia_connect.infraestructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import school.sptech.familia_connect.application.port.out.FamiliaPortOut;

@Repository
@RequiredArgsConstructor
public class FamiliaJdbcRepository implements FamiliaPortOut {

    public final JdbcTemplate jdbcTemplate;

    public int updateIdCarteirinhaById(String idCarteirinha, Integer id) {
        String sql = "UPDATE familia SET id_carteirinha = ? WHERE id = ?";

        return jdbcTemplate.update(sql, idCarteirinha, id);
    }

}
