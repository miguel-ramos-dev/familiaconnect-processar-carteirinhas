package school.sptech.familia_connect.application.service;


import lombok.AllArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;
import school.sptech.familia_connect.application.port.out.FamiliaPortOut;
import school.sptech.familia_connect.domain.Familia;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;

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

        String filePathQrCode = String.format("carteirinhas/%s.png",idCarteirinha);

        qrCodeService.downloadAndSaveQrCode(idCarteirinha, filePathQrCode);

        gerarCarteirinhaPdf(familiaEntity, "carteirinhas/example/carteirinha-example.pdf" , filePathQrCode);

        qrCodeService.deleteQrCode(filePathQrCode);
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

    private void gerarCarteirinhaPdf(Familia familiaEntity, String pathTemplateCarteirinha, String caminhoQrCode) {

        String[] palavrasNomeResponsavel = familiaEntity.getNomeResponsavel().split(" ");
        String nomeFamilia = palavrasNomeResponsavel[palavrasNomeResponsavel.length-1];

        //carrega documento
        try(PDDocument document = Loader.loadPDF(new File(pathTemplateCarteirinha))) {

            //pega primeira pagina
            PDPage page = document.getPage(0);

            //abre fluxo para inserir os textos de forma dinamica
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true
            )) {

                //Familia
                insertDataInDocument(contentStream, 7, 103, 92, nomeFamilia);

                // Responsável
                insertDataInDocument(contentStream, 7, 168, 92, familiaEntity.getNomeResponsavel());

                // CPF do Responsável
                insertDataInDocument(contentStream, 7, 103, 55, familiaEntity.getCpfResponsavel());

                // Telefone
                insertDataInDocument(contentStream, 7, 168, 55, familiaEntity.getTelefone());

                // Data
                DateTimeFormatter formatter = DateTimeFormatter
                        .ofLocalizedDate(FormatStyle.LONG)
                        .withLocale(Locale.of("pt", "BR"));
                insertDataInDocument(contentStream, 5, 183, 20, LocalDate.now().format(formatter));


                PDImageXObject qrCodeImage = PDImageXObject.createFromFile(caminhoQrCode, document);
                contentStream.drawImage(qrCodeImage, 304, 28, 120, 120);

            }

            document.save(String.format("carteirinhas/familia-%s.pdf", nomeFamilia));

        } catch (IOException e) {
            throw new RuntimeException("Erro ao manipular o PDF da carteirinha: " + e.getMessage(), e);
        }
    }

    private void insertDataInDocument(PDPageContentStream contentStream,
                                      int fontSize,
                                      int tx,
                                      int ty,
                                      String data
    ) throws IOException {
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        contentStream.beginText();
        contentStream.setFont(fontRegular, fontSize);
        contentStream.newLineAtOffset(tx, ty);
        contentStream.showText(data);
        contentStream.endText();
    }


}
