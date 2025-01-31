package ru.arsentiev.itemconsumer.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(); // Конвертер для сериализации и десериализации JSON
    }


    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // MANUAL => мы сами вызываем channel.basicAck(...)
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // Обработка в 3 потоках
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(3);

        // Prefetch = 3, чтобы не тянуть кучу сообщений, пока не ack'нуты
        factory.setPrefetchCount(3);

        return factory;
    }
}
