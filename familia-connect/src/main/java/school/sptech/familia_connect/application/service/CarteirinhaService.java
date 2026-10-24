package school.sptech.familia_connect.application.service;


import lombok.AllArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
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
import school.sptech.familia_connect.infraestructure.exception.ErrorEnum;
import school.sptech.familia_connect.infraestructure.exception.GlobalModelException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HexFormat;
import java.util.Locale;

@AllArgsConstructor
public class CarteirinhaService implements CarteirinhaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CarteirinhaService.class);

    private static final String PASTA_CARTEIRINHAS = "carteirinhas";
    private static final Object LOCK_PDF = new Object();

    // em pontos PDF (72 pt = 1 polegada, ~2,83 pt = 1 mm)
    private static final float ESPACO_ENTRE_CARTEIRINHAS = 15f;
    private static final float MARGEM_FOLHA = 15f;

    public FamiliaPortOut familiaPortOut;
    public QrCodeService qrCodeService;
    public LoteService loteService;

    /** Quantas carteirinhas cabem por folha. */
    private record Grade(int colunas, int linhas) {
        int capacidade() {
            return colunas * linhas;
        }
    }

    /** Posicao (canto inferior esquerdo) de uma carteirinha na folha. */
    private record Posicao(float x, float y) {
    }

    @Override
    public void criarCarteirinha(Familia familiaEntity) {
        log.info("[criarCarteirinha] - Iniciando processo de criação de carteirinha...");

        if (!familiaPortOut.existsById(familiaEntity.getIdFamilia())) {
            log.error("[findByIdFamilia] - Familia com id fornecido nao encontrado na base de dados, idFamilia={}", familiaEntity.getIdFamilia());
            throw new GlobalModelException();
        }

        String idCarteirinha = gerarIdCarteirinha(familiaEntity);
        log.info("[criarCarteirinha] - Identificador unico criado, idCarteirinha={}", idCarteirinha);

        String filePathQrCode = String.format("carteirinhas/%s.png",idCarteirinha);
        qrCodeService.downloadAndSaveQrCode(idCarteirinha, filePathQrCode);

        gerarCarteirinhaPdf(familiaEntity, "carteirinhas/example/carteirinha-example.pdf" , filePathQrCode);

        qrCodeService.deleteQrCode(filePathQrCode);

        updateIdCarteirinha(idCarteirinha, familiaEntity.getIdFamilia());
        log.info("[criarCarteirinha] - Identificador atualizado na base com sucesso!");
    }

    private void updateIdCarteirinha(String idCarteirinha, Integer idFamilia) {
        int idIsUpdated = familiaPortOut.updateIdCarteirinhaById(idCarteirinha, idFamilia);
        if(idIsUpdated <=0) {
            log.error("[updateIdCarteirinha] - Nenhuma linha atualizada em familia, idFamilia={}", idFamilia);
            throw new GlobalModelException();
        }
    }

    private String gerarIdCarteirinha(Familia familiaEntity) {
        log.info("[criarCarteirinha] - Gerando Identificador Unico da Carteirinha...");

        String valorCombinado = String.join("|",
                String.valueOf(familiaEntity.getIdFamilia()),
                familiaEntity.getCpfResponsavel().toLowerCase().trim(),
                familiaEntity.getTelefone().toLowerCase().trim(),
                familiaEntity.getNomeResponsavel().toLowerCase().trim()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(valorCombinado.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            log.error("[gerarIdCarteirinha] Erro ao carregar algoritmo de criptografia SHA-256");
            throw new GlobalModelException();
        }
    }

    // Orquestra: escolhe o lote, calcula a posicao, desenha e salva.
    private void gerarCarteirinhaPdf(Familia familiaEntity, String pathTemplateCarteirinha, String caminhoQrCode) {

        synchronized (LOCK_PDF) {
            try (PDDocument template = Loader.loadPDF(new File(pathTemplateCarteirinha))) {

                PDRectangle medidaCard = template.getPage(0).getMediaBox();
                Grade grade = calcularGrade(medidaCard);

                // uma pasta por dia: a contagem de lotes reinicia a cada data
                File pasta = new File(PASTA_CARTEIRINHAS, LocalDate.now().toString());
                File destino = loteService.obterPdfComEspaco(pasta, grade.capacidade());

                try (PDDocument lote = loteService.abrirOuCriarLote(destino)) {
                    int usados = loteService.lerSlots(lote);
                    Posicao posicao = calcularPosicao(usados, grade, medidaCard);

                    desenharCarteirinha(lote, template, posicao, familiaEntity, caminhoQrCode);

                    salvarLote(lote, destino, usados + 1);
                    log.info("[gerarCarteirinhaPdf] - Carteirinha inserida em {} (slot {}/{})",
                            destino.getName(), usados + 1, grade.capacidade());
                }
            } catch (IOException e) {
                log.error("[gerarCarteirinhaPdf] Erro ao gerar pdf da carteirinha, causa ={}", e.getMessage());
                throw new GlobalModelException();
            }
        }
    }

    private Grade calcularGrade(PDRectangle medidaCard) {
        float larguraUtil = PDRectangle.A4.getWidth() - 2 * MARGEM_FOLHA;
        float alturaUtil = PDRectangle.A4.getHeight() - 2 * MARGEM_FOLHA;

        int colunas = Math.max(1, (int) ((larguraUtil + ESPACO_ENTRE_CARTEIRINHAS)
                / (medidaCard.getWidth() + ESPACO_ENTRE_CARTEIRINHAS)));
        int linhas = Math.max(1, (int) ((alturaUtil + ESPACO_ENTRE_CARTEIRINHAS)
                / (medidaCard.getHeight() + ESPACO_ENTRE_CARTEIRINHAS)));
        return new Grade(colunas, linhas);
    }

    // centraliza a grade na horizontal; linhas preenchidas de cima para baixo, com espaco entre elas
    private Posicao calcularPosicao(int slot, Grade grade, PDRectangle medidaCard) {
        int coluna = slot % grade.colunas();
        int linha = slot / grade.colunas();

        float larguraGrade = grade.colunas() * medidaCard.getWidth()
                + (grade.colunas() - 1) * ESPACO_ENTRE_CARTEIRINHAS;
        float margemX = (PDRectangle.A4.getWidth() - larguraGrade) / 2;

        float x = margemX + coluna * (medidaCard.getWidth() + ESPACO_ENTRE_CARTEIRINHAS);
        float y = PDRectangle.A4.getHeight() - MARGEM_FOLHA
                - (linha + 1) * medidaCard.getHeight()
                - linha * ESPACO_ENTRE_CARTEIRINHAS;
        return new Posicao(x, y);
    }

    private void desenharCarteirinha(PDDocument lote,
                                     PDDocument template,
                                     Posicao posicao,
                                     Familia familiaEntity,
                                     String caminhoQrCode) throws IOException {

        // importa a pagina do template como "molde" desenhavel
        PDFormXObject molde = new LayerUtility(lote).importPageAsForm(template, 0);

        try (PDPageContentStream contentStream = new PDPageContentStream(
                lote, lote.getPage(0), PDPageContentStream.AppendMode.APPEND, true, true
        )) {
            // move a origem para o slot; dai pra frente usa as coordenadas originais do template
            contentStream.saveGraphicsState();
            contentStream.transform(Matrix.getTranslateInstance(posicao.x(), posicao.y()));

            contentStream.drawForm(molde);
            inserirDadosFamilia(contentStream, familiaEntity);
            inserirQrCode(contentStream, lote, caminhoQrCode);

            contentStream.restoreGraphicsState();
        }
    }

    private void inserirDadosFamilia(PDPageContentStream contentStream, Familia familiaEntity) throws IOException {
        //Familia
        insertDataInDocument(contentStream, 7, 104, 85, extrairNomeFamilia(familiaEntity));

        // Responsável
        insertDataInDocument(contentStream, 7, 170, 85, familiaEntity.getNomeResponsavel());

        // CPF do Responsável
        insertDataInDocument(contentStream, 7, 104, 48, familiaEntity.getCpfResponsavel());

        // Telefone
        insertDataInDocument(contentStream, 7, 170, 48, familiaEntity.getTelefone());

        // Data
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.of("pt", "BR"));
        insertDataInDocument(contentStream, 5, 183, 15, LocalDate.now().format(formatter));
    }

    private void inserirQrCode(PDPageContentStream contentStream, PDDocument lote, String caminhoQrCode) throws IOException {
        PDImageXObject qrCodeImage = PDImageXObject.createFromFile(caminhoQrCode, lote);
        contentStream.drawImage(qrCodeImage, 304, 20, 120, 120);
    }

    private String extrairNomeFamilia(Familia familiaEntity) {
        String[] palavras = familiaEntity.getNomeResponsavel().split(" ");
        return palavras[palavras.length - 1];
    }

    private void salvarLote(PDDocument lote, File destino, int slotsUsados) throws IOException {
        loteService.gravarSlots(lote, slotsUsados);
        Files.createDirectories(destino.toPath().getParent());
        lote.save(destino);
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
