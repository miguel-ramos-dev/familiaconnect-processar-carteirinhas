package school.sptech.familia_connect.application.service;

import lombok.AllArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import school.sptech.familia_connect.application.port.out.StoragePortOut;
import school.sptech.familia_connect.infraestructure.exception.GlobalModelException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsavel por gerenciar os lotes (PDFs A4 com varias carteirinhas):
 * localizar o lote com espaco, criar um novo e controlar quantas carteirinhas ja foram inseridas.
 * Os lotes sao persistidos atraves do StoragePortOut (localmente ou em um bucket S3), conforme configuracao.
 */
@AllArgsConstructor
public class LoteService {

    private static final String PREFIXO = "lote-";
    private static final String EXTENSAO = ".pdf";
    private static final String CHAVE_SLOTS = "slots";
    private static final Pattern PADRAO_LOTE = Pattern.compile("lote-(\\d+)\\.pdf");

    private final StoragePortOut storagePortOut;

    public String obterCaminhoLoteComEspaco(String pasta, int capacidade) {
        List<String> nomesLotes = storagePortOut.listarArquivos(pasta).stream()
                .filter(nome -> PADRAO_LOTE.matcher(nome).matches())
                .toList();

        if (nomesLotes.isEmpty()) {
            return caminho(pasta, 1);
        }

        String ultimoNome = nomesLotes.stream()
                .max(Comparator.comparingInt(this::numeroLote))
                .get();
        int ultimoNumero = numeroLote(ultimoNome);
        String ultimoCaminho = caminho(pasta, ultimoNumero);

        try (PDDocument d = Loader.loadPDF(storagePortOut.carregar(ultimoCaminho))) {
            if (lerSlots(d) < capacidade) return ultimoCaminho;
        } catch (IOException e) {
            throw new GlobalModelException();
        }
        return caminho(pasta, ultimoNumero + 1);
    }

    public PDDocument abrirOuCriarLote(String caminho) throws IOException {
        return storagePortOut.existe(caminho)
                ? Loader.loadPDF(storagePortOut.carregar(caminho))
                : criarLoteNovo();
    }

    public int lerSlots(PDDocument d) {
        String v = d.getDocumentInformation().getCustomMetadataValue(CHAVE_SLOTS);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public void gravarSlots(PDDocument d, int n) {
        d.getDocumentInformation().setCustomMetadataValue(CHAVE_SLOTS, String.valueOf(n));
    }

    public void salvarLote(PDDocument lote, String caminho, int slotsUsados) throws IOException {
        gravarSlots(lote, slotsUsados);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            lote.save(out);
            storagePortOut.salvar(caminho, out.toByteArray());
        }
    }

    private String caminho(String pasta, int numero) {
        return pasta + "/" + PREFIXO + numero + EXTENSAO;
    }

    private int numeroLote(String nomeArquivo) {
        Matcher matcher = PADRAO_LOTE.matcher(nomeArquivo);
        matcher.matches();
        return Integer.parseInt(matcher.group(1));
    }

    private PDDocument criarLoteNovo() {
        PDDocument d = new PDDocument();
        d.addPage(new PDPage(PDRectangle.A4));
        return d;
    }
}
