package school.sptech.familia_connect.infraestructure.storage;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import school.sptech.familia_connect.application.port.out.StoragePortOut;
import school.sptech.familia_connect.infraestructure.exception.GlobalModelException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Adapter que persiste os arquivos no sistema de arquivos local.
 * Usado quando a propriedade app.storage.local-enabled estiver como true.
 */
@AllArgsConstructor
public class LocalStorageAdapter implements StoragePortOut {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageAdapter.class);

    @Override
    public void salvar(String caminho, byte[] conteudo) {
        try {
            Path path = Path.of(caminho);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.write(path, conteudo);
        } catch (IOException e) {
            log.error("[salvar] Erro ao salvar arquivo localmente, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public byte[] carregar(String caminho) {
        try {
            return Files.readAllBytes(Path.of(caminho));
        } catch (IOException e) {
            log.error("[carregar] Erro ao carregar arquivo local, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public boolean existe(String caminho) {
        return Files.exists(Path.of(caminho));
    }

    @Override
    public void deletar(String caminho) {
        try {
            Files.deleteIfExists(Path.of(caminho));
        } catch (IOException e) {
            log.error("[deletar] Erro ao deletar arquivo local, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public List<String> listarArquivos(String pasta) {
        Path path = Path.of(pasta);
        if (!Files.exists(path)) {
            return List.of();
        }
        try (Stream<Path> arquivos = Files.list(path)) {
            return arquivos
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("[listarArquivos] Erro ao listar arquivos locais, pasta={}, causa={}", pasta, e.getMessage());
            throw new GlobalModelException();
        }
    }
}
