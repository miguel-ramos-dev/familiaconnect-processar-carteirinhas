package school.sptech.familia_connect.infraestructure.consumer.mapper;

import org.mapstruct.Mapper;
import school.sptech.familia_connect.domain.Familia;
import school.sptech.familia_connect.infraestructure.consumer.dto.FamiliaRequestDto;

@Mapper(componentModel = "spring")
public interface FamiliaMapper {

    Familia toEntity(FamiliaRequestDto requestDto);

}
