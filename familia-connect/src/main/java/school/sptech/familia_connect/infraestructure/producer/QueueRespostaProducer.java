package school.sptech.familia_connect.infraestructure.producer;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import school.sptech.familia_connect.infraestructure.config.RabbitMQConfig;
import school.sptech.familia_connect.infraestructure.producer.dto.QueueRespostaDto;

@Component
@AllArgsConstructor
public class QueueRespostaProducer {

    private static final Logger log = LoggerFactory.getLogger(QueueRespostaProducer.class);

    private RabbitTemplate rabbitTemplate;
    private RabbitMQConfig rabbitMQConfig;

    public void enviar(QueueRespostaDto queueRespostaDto, String correlationIdRedis) {
        log.info("({}) [enviar] - Resposta de processamento sendo enviada para a fila = {}\n Iniciando processamento...", QueueRespostaProducer.class, rabbitMQConfig.getQUEUE_RESPOSTA_NAME());

        rabbitTemplate.convertAndSend(
                rabbitMQConfig.getEXCHANGE_NAME(),
                rabbitMQConfig.getROUTING_KEY_RESPOSTA(),
                queueRespostaDto,
                message -> {
                    message.getMessageProperties().setHeader("correlationIdRedis", correlationIdRedis);
                    return message;
                }
        );
        log.info("({}) [enviar] - Resposta enviada com sucesso!\nPayload = {}", QueueRespostaProducer.class , queueRespostaDto);

    }
}
