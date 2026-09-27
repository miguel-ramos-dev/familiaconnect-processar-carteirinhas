package school.sptech.familia_connect.infraestructure.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import school.sptech.familia_connect.application.port.in.CarteirinhaUseCase;
import school.sptech.familia_connect.domain.Familia;
import school.sptech.familia_connect.infraestructure.consumer.dto.FamiliaRequestDto;
import school.sptech.familia_connect.infraestructure.consumer.mapper.FamiliaMapper;

@Component
@RequiredArgsConstructor
public class QueueCarteirinhasConsumer {

    private static final Logger log = LoggerFactory.getLogger(QueueCarteirinhasConsumer.class);

    public final CarteirinhaUseCase carteirinhaUseCase;
    public final FamiliaMapper familiaMapper;

    @RabbitListener(
            queues = "${spring.rabbitmq.queue-name}",
            messageConverter = "messageConverter"
    )
    public void onMessage(FamiliaRequestDto familiaRequest) {
        log.info("({}) [onMessage] - Mensagem recebida da fila com sucesso!\n Iniciando processamento...", QueueCarteirinhasConsumer.class);

        Familia familiaEntity = familiaMapper.toEntity(familiaRequest);
        log.info("({}) [onMessage] - Conversão bem-sucedida dos dados de entrada!", QueueCarteirinhasConsumer.class);

        carteirinhaUseCase.criarCarteirinha(familiaEntity);
    }

}
