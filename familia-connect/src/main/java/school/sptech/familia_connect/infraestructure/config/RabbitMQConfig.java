package school.sptech.familia_connect.infraestructure.config;

import lombok.Getter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.queue-name}")
    public String QUEUE_NAME;

    @Value("${spring.rabbitmq.producer.queue-name}")
    public String QUEUE_RESPOSTA_NAME;

    @Value("${spring.rabbitmq.listener.exchange-name}")
    public String EXCHANGE_NAME;

    @Value("${spring.rabbitmq.listener.binding-name}")
    public String BINDING_NAME;

    @Value("${spring.rabbitmq.producer.routing-name}")
    public String ROUTING_KEY_RESPOSTA;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .build();
    }

    @Bean
    public Queue queueResposta() {
        return QueueBuilder
                .durable(QUEUE_RESPOSTA_NAME)
                .build();
    }
    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(BINDING_NAME);
    }

    @Bean
    public Binding routingKeyQueueResposta(Queue queueResposta, DirectExchange exchange) {
        return BindingBuilder
                .bind(queueResposta)
                .to(exchange)
                .with(ROUTING_KEY_RESPOSTA);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
