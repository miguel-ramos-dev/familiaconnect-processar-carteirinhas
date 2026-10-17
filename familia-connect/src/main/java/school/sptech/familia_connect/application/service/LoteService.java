package school.sptech.familia_connect.application.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Responsavel por gerenciar os lotes (PDFs A4 com varias carteirinhas):
 * localizar o lote com espaco, criar um novo e controlar quantas carteirinhas ja foram inseridas.
 */
public class LoteService {

    private static final String PREFIXO = "lote-";
    private static final String EXTENSAO = ".pdf";
    private static final String CHAVE_SLOTS = "slots";

    public File obterPdfComEspaco(File pasta, int capacidade) throws IOException {
        File[] lotes = pasta.listFiles((d, n) -> n.matches("lote-\\d+\\.pdf"));
        if (lotes == null || lotes.length == 0) {
            return new File(pasta, PREFIXO + 1 + EXTENSAO);
        }

        File ultimo = Arrays.stream(lotes)
                .max(Comparator.comparingInt(this::numeroLote)).get();

        try (PDDocument d = Loader.loadPDF(Files.readAllBytes(ultimo.toPath()))) {
            if (lerSlots(d) < capacidade) return ultimo;
        }
        return new File(pasta, PREFIXO + (numeroLote(ultimo) + 1) + EXTENSAO);
    }

    public PDDocument abrirOuCriarLote(File destino) throws IOException {
        return destino.exists()
                ? Loader.loadPDF(Files.readAllBytes(destino.toPath()))
                : criarLoteNovo();
    }

    public int lerSlots(PDDocument d) {
        String v = d.getDocumentInformation().getCustomMetadataValue(CHAVE_SLOTS);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public void gravarSlots(PDDocument d, int n) {
        d.getDocumentInformation().setCustomMetadataValue(CHAVE_SLOTS, String.valueOf(n));
    }

    private int numeroLote(File f) {
        String n = f.getName();
        return Integer.parseInt(n.substring(PREFIXO.length(), n.length() - EXTENSAO.length()));
    }

    private PDDocument criarLoteNovo() {
        PDDocument d = new PDDocument();
        d.addPage(new PDPage(PDRectangle.A4));
        return d;
    }
}

