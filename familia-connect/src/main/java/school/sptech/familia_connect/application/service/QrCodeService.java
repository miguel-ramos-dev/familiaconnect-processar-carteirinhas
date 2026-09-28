package school.sptech.familia_connect.application.service;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import school.sptech.familia_connect.infraestructure.web.client.CreateQrCodeClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@AllArgsConstructor
public class QrCodeService {

    private final Logger log = LoggerFactory.getLogger(QrCodeService.class);

    private final CreateQrCodeClient createQrCodeClient;

    public void downloadAndSaveQrCode(String conteudo, String filePath) {
        log.info("({}) [downloadAndSaveQrCode] - Iniciando processo de criacao de carteirinha", QrCodeService.class);
        try {
            byte[] imageBytes = createQrCodeClient.gerarQrCode("300x300", conteudo);
            if(imageBytes != null && imageBytes.length > 0) {
                log.info("({}) [downloadAndSaveQrCode] - Carteirinha criada com sucesso", QrCodeService.class);

                Path path = Paths.get(filePath);

                Path parentDir = path.getParent();
                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                Files.write(path, imageBytes);
                log.info("({}) [downloadAndSaveQrCode] - Carteirinhas salva em pasta local /carteirinhas", QrCodeService.class);

            }
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao salvar o arquivo localmente: " + ex.getMessage(), ex);        }
    }

}
