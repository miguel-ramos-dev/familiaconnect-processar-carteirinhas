package school.sptech.familia_connect.infraestructure.consumer.mapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import school.sptech.familia_connect.domain.Familia;
import school.sptech.familia_connect.infraestructure.consumer.dto.FamiliaRequestDto;
import school.sptech.familia_connect.infraestructure.exception.CamposInvalidosException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Mapper(componentModel = "spring")
public abstract class FamiliaMapper {

    @Autowired
    protected Validator validator;

    public abstract Familia toEntity(FamiliaRequestDto requestDto);

    @BeforeMapping
    protected void verifyRequestDto(FamiliaRequestDto requestDto) {
        if(requestDto == null) {
            throw new NullPointerException("Dados familia inexistentes, nulos");
        }
        Set<ConstraintViolation<FamiliaRequestDto>> violations = validator.validate(requestDto);

        if(!violations.isEmpty()) {
            Map<String, String> erros = new HashMap<>();
            for(ConstraintViolation<FamiliaRequestDto> violation : violations) {
                erros.put(violation.getPropertyPath().toString(), violation.getMessage());
            }

            throw new CamposInvalidosException("Falha na validacao dos dados da familia", erros);
        }

    }

}
