package school.sptech.familia_connect.application.service;


import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;
import school.sptech.familia_connect.application.port.out.FamiliaPortOut;
import school.sptech.familia_connect.domain.Familia;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@AllArgsConstructor
public class CarteirinhaService implements CarteirinhaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CarteirinhaService.class);

    public FamiliaPortOut familiaPortOut;
    public QrCodeService qrCodeService;

    @Override
    public void criarCarteirinha(Familia familiaEntity) {
        log.info("({}) [criarCarteirinha] - Iniciando processo de criação de carteirinha...", CarteirinhaService.class);

        String idCarteirinha = gerarIdCarteirinha(familiaEntity);
        log.info("({}) [criarCarteirinha] - Identificador unico criado, idCarteirinha={}", CarteirinhaService.class, idCarteirinha);

        updateIdCarteirinha(idCarteirinha, familiaEntity.getId());
        log.info("({}) [criarCarteirinha] - Identificador atualizado na base com sucesso!", CarteirinhaService.class);

        qrCodeService.downloadAndSaveQrCode(idCarteirinha, "carteirinhas/" + idCarteirinha + ".png");
    }

    private void updateIdCarteirinha(String idCarteirinha, Integer idFamilia) {
        int idIsUpdated = familiaPortOut.updateIdCarteirinhaById(idCarteirinha, idFamilia);
        if(idIsUpdated <=0) throw new RuntimeException("Erro ao atualizar id da carteirinha no banco de dados");
    }

    private String gerarIdCarteirinha(Familia familiaEntity) {
        log.info("({}) [criarCarteirinha] - Gerando Identificador Unico da Carteirinha...", CarteirinhaService.class);

        String valorCombinado = String.join("|",
                String.valueOf(familiaEntity.getId()),
                familiaEntity.getCpfResponsavel().toLowerCase().trim(),
                familiaEntity.getTelefone().toLowerCase().trim(),
                familiaEntity.getNomeResponsavel().toLowerCase().trim()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(valorCombinado.getBytes(StandardCharsets.UTF_8));


            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Erro ao carregar algoritmo de criptografia SHA-256");
        }
    }

}
