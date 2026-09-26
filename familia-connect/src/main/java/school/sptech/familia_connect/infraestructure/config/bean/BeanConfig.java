package school.sptech.familia_connect.infraestructure.config.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import school.sptech.familia_connect.application.impl.CarteirinhaImpl;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;

@Configuration
public class BeanConfig {

    @Bean
    public CarteirinhaUseCase carteirinhaUseCase() {
        return new CarteirinhaImpl();
    }
}
