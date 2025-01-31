package ru.arsentiev.producer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    // Читаем из application.yaml
    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUser;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    // Для Item (Direct Exchange)
    @Value("${app.rabbit.item.exchange}")
    private String itemExchangeName;
    @Value("${app.rabbit.item.routing-key}")
    private String itemRoutingKey;

    // Для Card (Topic Exchange)
    @Value("${app.rabbit.card.exchange}")
    private String cardExchangeName;
    @Value("${app.rabbit.card.routing-key-template}")
    private String cardRoutingKeyTemplate;

    @Value("${app.rabbit.item.name-queue}")
    private String itemQueue;
    @Value("${app.rabbit.card.name-queue}")
    private String cardQueue;

    /**
     * 1. Фабрика соединений с RabbitMQ.
     * Настройка кэширования, хост, порт, логин/пароль и пр.
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        factory.setUsername(rabbitUser);
        factory.setPassword(rabbitPassword);

        // включаем publisher confirms и returns
        // чтобы RabbitTemplate знал о необходимости callbacks.
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);

        return factory;
    }

    /**
     * 2. Direct Exchange для Item
     * durable=true, autoDelete=false
     */
    @Bean
    public Exchange itemDirectExchange() {
        return ExchangeBuilder
                .directExchange(itemExchangeName)
                .durable(true)
                .build();
    }

    /**
     * 3. Topic Exchange для Card
     * durable=true, autoDelete=false
     */
    @Bean
    public Exchange cardTopicExchange() {
        return ExchangeBuilder
                .topicExchange(cardExchangeName)
                .durable(true)
                .build();
    }

    /**
     * 4. Очередь для Item
     */
    @Bean
    public Queue itemQueue() {
        return QueueBuilder
                .durable(itemQueue)
                .build();
    }

    /**
     * 5. Очередь для Card
     */
    @Bean
    public Queue cardQueue() {
        return QueueBuilder
                .durable(cardQueue)
                .build();
    }

    /**
     * 6. Binding для ItemQueue -> Direct Exchange
     * Привязываемся по ключу itemRoutingKey
     */
    @Bean
    public Binding itemQueueBinding() {
        return BindingBuilder
                .bind(itemQueue())
                .to(itemDirectExchange())
                .with(itemRoutingKey)
                .noargs();
    }

    /**
     * 7. Binding для CardQueue -> Topic Exchange
     * Будем слушать все сообщения с routing key "card.*"
     */
    @Bean
    public Binding cardQueueBinding() {
        return BindingBuilder
                .bind(cardQueue())
                .to(cardTopicExchange())
                .with(cardRoutingKeyTemplate) // "card.#"
                .noargs();
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 8. RabbitTemplate
     * - Устанавливаем callbacks для подтверждения (confirm) и возврата сообщений (return).
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // Если сообщение не может быть доставлено по binding (нет очереди или нет rountingKey),
        // оно вернётся в callback. Нужно также включить Mandatory или publisher-returns.
        template.setMandatory(true);

        template.setMessageConverter(messageConverter);

        // Confirm Callback: вызывается, когда broker подтвердил получение (ack) или не подтвердил (nack).
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("ConfirmCallback: Сообщение подтверждено broker'ом! {}",
                        correlationData != null ? correlationData.getId() : "");
            } else {
                log.warn(">>> ConfirmCallback: Сообщение НЕ подтверждено! Причина: {}", cause);
            }
        });

        // Return Callback: вызывается, если сообщение не было маршрутизировано ни в одну очередь.
        template.setReturnsCallback(returned -> {
            log.warn(">>> ReturnCallback: Сообщение возвращено! replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getReplyCode(), returned.getReplyText(), returned.getExchange(), returned.getRoutingKey());
        });

        return template;
    }
}