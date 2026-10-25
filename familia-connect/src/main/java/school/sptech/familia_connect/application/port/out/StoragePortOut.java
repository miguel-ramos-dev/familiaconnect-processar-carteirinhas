package school.sptech.familia_connect.application.port.out;

import java.util.List;

/**
 * Abstrai onde os arquivos gerados (QR Codes e lotes de carteirinhas) sao persistidos.
 * A implementacao usada (local ou S3) e escolhida via propriedade de configuracao.
 */
public interface StoragePortOut {

    void salvar(String caminho, byte[] conteudo);

    byte[] carregar(String caminho);

    boolean existe(String caminho);

    void deletar(String caminho);

    /**
     * Lista os nomes dos arquivos existentes dentro de uma pasta/prefixo.
     */
    List<String> listarArquivos(String pasta);
}
