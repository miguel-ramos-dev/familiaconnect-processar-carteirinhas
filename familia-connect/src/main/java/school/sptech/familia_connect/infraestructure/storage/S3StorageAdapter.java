package school.sptech.familia_connect.infraestructure.storage;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import school.sptech.familia_connect.application.port.out.StoragePortOut;
import school.sptech.familia_connect.infraestructure.exception.GlobalModelException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter que persiste os arquivos em um bucket S3 da AWS.
 * Usado quando a propriedade app.storage.local-enabled estiver como false.
 */
@AllArgsConstructor
public class S3StorageAdapter implements StoragePortOut {

    private static final Logger log = LoggerFactory.getLogger(S3StorageAdapter.class);

    private final S3Client s3Client;
    private final String bucket;

    @Override
    public void salvar(String caminho, byte[] conteudo) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(caminho)
                            .build(),
                    RequestBody.fromBytes(conteudo)
            );
        } catch (S3Exception e) {
            log.error("[salvar] Erro ao salvar arquivo no S3, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public byte[] carregar(String caminho) {
        try {
            ResponseBytes<GetObjectResponse> objeto = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(caminho)
                            .build()
            );
            return objeto.asByteArray();
        } catch (S3Exception e) {
            log.error("[carregar] Erro ao carregar arquivo do S3, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public boolean existe(String caminho) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(caminho)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("[existe] Erro ao verificar existencia do arquivo no S3, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public void deletar(String caminho) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(caminho)
                            .build()
            );
        } catch (S3Exception e) {
            log.error("[deletar] Erro ao deletar arquivo do S3, caminho={}, causa={}", caminho, e.getMessage());
            throw new GlobalModelException();
        }
    }

    @Override
    public List<String> listarArquivos(String pasta) {
        try {
            String prefixo = pasta.endsWith("/") ? pasta : pasta + "/";
            ListObjectsV2Response resposta = s3Client.listObjectsV2(
                    ListObjectsV2Request.builder()
                            .bucket(bucket)
                            .prefix(prefixo)
                            .build()
            );
            return resposta.contents().stream()
                    .map(obj -> obj.key().substring(prefixo.length()))
                    .filter(nome -> !nome.isBlank())
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            log.error("[listarArquivos] Erro ao listar arquivos no S3, pasta={}, causa={}", pasta, e.getMessage());
            throw new GlobalModelException();
        }
    }
}
