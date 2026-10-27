package school.sptech.familia_connect.infraestructure.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;
import school.sptech.familia_connect.domain.Familia;
import school.sptech.familia_connect.infraestructure.consumer.dto.FamiliaRequestDto;
import school.sptech.familia_connect.infraestructure.consumer.mapper.FamiliaMapper;
import school.sptech.familia_connect.infraestructure.exception.GlobalModelException;
import school.sptech.familia_connect.infraestructure.producer.QueueRespostaProducer;
import school.sptech.familia_connect.infraestructure.producer.dto.QueueRespostaDto;

@Component
@RequiredArgsConstructor
public class QueueCarteirinhasConsumer {

    private static final Logger log = LoggerFactory.getLogger(QueueCarteirinhasConsumer.class);

    public final CarteirinhaUseCase carteirinhaUseCase;
    public final FamiliaMapper familiaMapper;
    public final QueueRespostaProducer queueRespostaProducer;

    @RabbitListener(
            queues = "${spring.rabbitmq.queue-name}",
            messageConverter = "messageConverter"
    )
    public void onMessage(@Header(value = "correlationIdRedis", required = false) String correlationIdRedis, FamiliaRequestDto familiaRequest) {
        QueueRespostaDto queueRespostaDto = new QueueRespostaDto("SUCESSO", "Carteirinha da familia gerada com sucesso!", familiaRequest.nomeResponsavel());
        log.info("[onMessage] - Mensagem recebida da fila com sucesso!\n Iniciando processamento...");

        if(correlationIdRedis == null || correlationIdRedis.isBlank()) {
            log.error("[verifyHeaderRedis] Falta de header redis na mensagem consumida... Encerrando fluxo");
            return;
        }
        try {
            Familia familiaEntity = familiaMapper.toEntity(familiaRequest);
            log.info("[onMessage] - Conversão bem-sucedida dos dados de entrada!");

            carteirinhaUseCase.criarCarteirinha(familiaEntity);

            log.info("[onMessage] Sucesso no fluxo de criação de carteirinhas!");

        } catch (GlobalModelException e) {
            log.error("[onMessage] Erro no fluxo de criação de carteirinhas, causa = {}", e.getMessage());
            queueRespostaDto.setMessage(e.getMessage());
            queueRespostaDto.setStatus(e.getStatus().toString());
        } finally {
            queueRespostaProducer.enviar(queueRespostaDto, correlationIdRedis);
        }

    }

}
