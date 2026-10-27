package school.sptech.familia_connect.infraestructure.config.bean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import school.sptech.familia_connect.application.port.out.FamiliaPortOut;
import school.sptech.familia_connect.application.port.out.StoragePortOut;
import school.sptech.familia_connect.application.service.CarteirinhaService;
import school.sptech.familia_connect.application.service.LoteService;
import school.sptech.familia_connect.application.service.QrCodeService;
import school.sptech.familia_connect.infraestructure.storage.LocalStorageAdapter;
import school.sptech.familia_connect.infraestructure.storage.S3StorageAdapter;
import school.sptech.familia_connect.infraestructure.web.client.CreateQrCodeClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class BeanConfig {

    @Bean
    public CarteirinhaService carteirinhaUseCase(FamiliaPortOut familiaPortOut,
                                                 QrCodeService qrCodeService,
                                                 LoteService loteService) {
        return new CarteirinhaService(familiaPortOut, qrCodeService, loteService);
    }

    @Bean
    public LoteService loteService(StoragePortOut storagePortOut) {
        return new LoteService(storagePortOut);
    }

    // Com aviso porque spring so faz a impl dessa interface em runtime.
    @Bean
    public QrCodeService qrCodeService(CreateQrCodeClient createQrCodeClient) {
        return new QrCodeService(createQrCodeClient);
    }

    // app.storage.local-enabled=true (padrao) -> salva os arquivos localmente
    @Bean
    @ConditionalOnProperty(prefix = "app.storage", name = "local-enabled", havingValue = "true", matchIfMissing = true)
    public StoragePortOut localStorageAdapter() {
        return new LocalStorageAdapter();
    }

    // app.storage.local-enabled=false -> salva os arquivos em um bucket S3 da AWS
    @Bean
    @ConditionalOnProperty(prefix = "app.storage", name = "local-enabled", havingValue = "false")
    public StoragePortOut s3StorageAdapter(@Value("${app.storage.s3.region}") String region,
                                            @Value("${app.storage.s3.bucket}") String bucket) {
        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        return new S3StorageAdapter(s3Client, bucket);
    }

}
