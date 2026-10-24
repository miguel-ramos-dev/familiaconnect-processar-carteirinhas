package school.sptech.familia_connect.infraestructure.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueueRespostaDto{
    private String status;
    private String message;
}
