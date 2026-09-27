package school.sptech.familia_connect.infraestructure.consumer.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FamiliaRequestDto(

        @NotNull(message = "O campo idFamilia não pode ser nulo")
        Integer id,

        @NotBlank(message = "O campo nomeResponsavel não pode ser nulo")
        String nomeResponsavel,

        @NotBlank(message = "O campo cpfResponsavel não pode ser nulo")
        String cpfResponsavel,

        @NotBlank(message = "O campo telefone não pode ser nulo")
        String telefone
) {
}
