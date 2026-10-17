package school.sptech.familia_connect.infraestructure.config.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import school.sptech.familia_connect.application.port.out.FamiliaPortOut;
import school.sptech.familia_connect.application.service.CarteirinhaService;
import school.sptech.familia_connect.application.service.LoteService;
import school.sptech.familia_connect.application.service.QrCodeService;
import school.sptech.familia_connect.infraestructure.web.client.CreateQrCodeClient;

@Configuration
public class BeanConfig {

    @Bean
    public CarteirinhaService carteirinhaUseCase(FamiliaPortOut familiaPortOut,
                                                 QrCodeService qrCodeService,
                                                 LoteService loteService) {
        return new CarteirinhaService(familiaPortOut, qrCodeService, loteService);
    }

    @Bean
    public LoteService loteService() {
        return new LoteService();
    }

    // Com aviso porque spring so faz a impl dessa interface em runtime.
    @Bean
    public QrCodeService qrCodeService(CreateQrCodeClient createQrCodeClient) {
        return new QrCodeService(createQrCodeClient);
    }

}
