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
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;
import school.sptech.familia_connect.application.port.out.FamiliaPortOut;
import school.sptech.familia_connect.domain.Familia;
import school.sptech.familia_connect.infraestructure.exception.ErrorEnum;
import school.sptech.familia_connect.infraestructure.exception.GlobalModelException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    private static final Color COR_AZUL = new Color(0x1E, 0x47, 0x90);
    private static final Color COR_TEXTO = new Color(0x11, 0x11, 0x11);
    private static final float X_FIM_DATA = 224f;

    // area branca do QR code no template e folga entre ela e o QR
    private static final float QR_AREA_X = 301f;
    private static final float QR_AREA_Y = 17f;
    private static final float QR_AREA_TAMANHO = 128f;
    private static final float QR_MARGEM = 14f;

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

        familiaEntity.setQuantidadeMembros(familiaPortOut.countMembrosByIdFamilia(familiaEntity.getIdFamilia()));

        String idCarteirinha = gerarIdCarteirinha(familiaEntity);
        log.info("[criarCarteirinha] - Identificador unico criado, idCarteirinha={}", idCarteirinha);

        String filePathQrCode = String.format("carteirinhas/%s.png",idCarteirinha);
        qrCodeService.downloadAndSaveQrCode(idCarteirinha, filePathQrCode);

        gerarCarteirinhaPdf(familiaEntity, "carteirinhas/example/carteirinha-example.pdf", filePathQrCode);

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
                String pasta = PASTA_CARTEIRINHAS + "/" + LocalDate.now();
                String destino = loteService.obterCaminhoLoteComEspaco(pasta, grade.capacidade());

                try (PDDocument lote = loteService.abrirOuCriarLote(destino)) {
                    int usados = loteService.lerSlots(lote);
                    Posicao posicao = calcularPosicao(usados, grade, medidaCard);

                    desenharCarteirinha(lote, template, posicao, familiaEntity, caminhoQrCode);

                    loteService.salvarLote(lote, destino, usados + 1);
                    log.info("[gerarCarteirinhaPdf] - Carteirinha inserida em {} (slot {}/{})",
                            destino, usados + 1, grade.capacidade());
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

    private record Fontes(PDFont medium, PDFont semiBold) {
    }

    private PDFont carregarFonte(PDDocument doc, String nome) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fonts/" + nome)) {
            if (in == null) {
                throw new IOException("Fonte nao encontrada: " + nome);
            }
            return PDType0Font.load(doc, in);
        }
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

            Fontes fontes = new Fontes(carregarFonte(lote, "Poppins-Medium.ttf"), carregarFonte(lote, "Poppins-SemiBold.ttf"));

            contentStream.drawForm(molde);
            inserirDadosFamilia(contentStream, fontes, familiaEntity);
            inserirQrCode(contentStream, lote, caminhoQrCode);

            contentStream.restoreGraphicsState();
        }
    }

    private void inserirDadosFamilia(PDPageContentStream contentStream, Fontes fontes, Familia familiaEntity) throws IOException {
        // Data de emissao, alinhada a direita
        String dataEmissao = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        float larguraData = fontes.semiBold().getStringWidth(dataEmissao) / 1000 * 8;
        insertDataInDocument(contentStream, fontes.semiBold(), 8, COR_AZUL, X_FIM_DATA - larguraData, 140, dataEmissao);

        // Familia
        insertDataInDocument(contentStream, fontes.semiBold(), 18, COR_AZUL, 13, 103,
                "Família " + extrairNomeFamilia(familiaEntity));

        inserirBadgeMembros(contentStream, fontes, familiaEntity.getQuantidadeMembros());

        // Responsável, CPF e Telefone
        float y = 61;
        for (String linha : quebrarTexto(fontes.medium(), 7.5f, 74, familiaEntity.getNomeResponsavel())) {
            insertDataInDocument(contentStream, fontes.medium(), 7.5f, COR_TEXTO, 13, y, linha);
            y -= 9;
        }
        insertDataInDocument(contentStream, fontes.medium(), 7.5f, COR_TEXTO, 93, 61, familiaEntity.getCpfResponsavel());
        insertDataInDocument(contentStream, fontes.medium(), 7.5f, COR_TEXTO, 172, 61, familiaEntity.getTelefone());
    }

    private java.util.List<String> quebrarTexto(PDFont font, float fontSize, float larguraMaxima, String texto) throws IOException {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.trim().split("\\s+")) {
            String candidata = atual.length() == 0 ? palavra : atual + " " + palavra;
            if (atual.length() > 0 && font.getStringWidth(candidata) / 1000 * fontSize > larguraMaxima) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(candidata);
            }
        }
        linhas.add(atual.toString());
        return linhas;
    }

    private void inserirBadgeMembros(PDPageContentStream contentStream, Fontes fontes, Integer quantidade) throws IOException {
        int total = quantidade == null ? 0 : quantidade;
        String texto = total + (total == 1 ? " membro" : " membros");

        float largura = fontes.semiBold().getStringWidth(texto) / 1000 * 6 + 2 * 9;
        float altura = 10.3f;
        float x = 13;
        float y = 85;

        // retangulo com cantos totalmente arredondados (pilula)
        float raio = altura / 2;
        float k = 0.5523f * raio;
        contentStream.setNonStrokingColor(COR_AZUL);
        contentStream.moveTo(x + raio, y);
        contentStream.lineTo(x + largura - raio, y);
        contentStream.curveTo(x + largura - raio + k, y, x + largura, y + raio - k, x + largura, y + raio);
        contentStream.curveTo(x + largura, y + raio + k, x + largura - raio + k, y + altura, x + largura - raio, y + altura);
        contentStream.lineTo(x + raio, y + altura);
        contentStream.curveTo(x + raio - k, y + altura, x, y + raio + k, x, y + raio);
        contentStream.curveTo(x, y + raio - k, x + raio - k, y, x + raio, y);
        contentStream.fill();

        insertDataInDocument(contentStream, fontes.semiBold(), 6, Color.WHITE, x + 9, y + 3.5f, texto);
    }

    private void inserirQrCode(PDPageContentStream contentStream, PDDocument lote, String caminhoQrCode) throws IOException {
        PDImageXObject qrCodeImage = PDImageXObject.createFromFile(caminhoQrCode, lote);
        contentStream.drawImage(qrCodeImage, QR_AREA_X + QR_MARGEM, QR_AREA_Y + QR_MARGEM,
                QR_AREA_TAMANHO - 2 * QR_MARGEM, QR_AREA_TAMANHO - 2 * QR_MARGEM);
    }

    private String extrairNomeFamilia(Familia familiaEntity) {
        String[] palavras = familiaEntity.getNomeResponsavel().split(" ");
        return palavras[palavras.length - 1];
    }

    private void insertDataInDocument(PDPageContentStream contentStream,
                                      PDFont font,
                                      float fontSize,
                                      Color cor,
                                      float tx,
                                      float ty,
                                      String data
    ) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(cor);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(tx, ty);
        contentStream.showText(data);
        contentStream.endText();
    }

}