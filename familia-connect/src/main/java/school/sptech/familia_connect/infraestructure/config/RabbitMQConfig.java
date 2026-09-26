package school.sptech.familia_connect.infraestructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.queue-name}")
    public String QUEUE_NAME;

    @Value("${spring.rabbitmq.listener.exchange-name}")
    public String EXCHANGE_NAME;

    @Value("${spring.rabbitmq.listener.binding-name}")
    public String BINDING_NAME;

    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(BINDING_NAME);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
