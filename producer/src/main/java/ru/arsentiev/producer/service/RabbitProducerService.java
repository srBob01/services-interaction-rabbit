package ru.arsentiev.producer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.arsentiev.producer.entity.Card;
import ru.arsentiev.producer.entity.Item;

import static org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT;

@Service
@RequiredArgsConstructor
public class RabbitProducerService {

    private final RabbitTemplate rabbitTemplate;

    // Из app.rabbit.item в application.yaml
    @Value("${app.rabbit.item.exchange}")
    private String itemExchange;
    @Value("${app.rabbit.item.routing-key}")
    private String itemRoutingKey;

    // Из app.rabbit.card в application.yaml
    @Value("${app.rabbit.card.exchange}")
    private String cardExchange;
    @Value("${app.rabbit.card.routing-key}")
    private String cardRoutingKey;

    /**
     * Отправка Item в Direct Exchange
     */
    public void sendItem(Item item) {
        // Можно добавлять correlationData (ID), чтобы трекать в confirm callback
        rabbitTemplate.convertAndSend(
                itemExchange,
                itemRoutingKey,
                item,
                message -> {
                    message.getMessageProperties().setDeliveryMode(PERSISTENT);
                    return message;
                }
        );
    }

    /**
     * Отправка Card в Topic Exchange
     */
    public void sendCard(Card card) {
        rabbitTemplate.convertAndSend(
                cardExchange,
                cardRoutingKey,
                card,
                message -> {
                    message.getMessageProperties().setDeliveryMode(PERSISTENT);
                    return message;
                }
        );
    }
}
