package ru.arsentiev.cardconsumer.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.arsentiev.cardconsumer.model.Card;
import ru.arsentiev.cardconsumer.service.SmsService;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardListener {

    private final SmsService smsService;

    @RabbitListener(queues = "${app.rabbit.card.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void receiveCard(Card card,
                            Message message,
                            Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("Received a message about Card: {}", card);

            String phoneNumber = card.getOwner().getPhoneNumber();

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("The owner of {} does not have a phone number, no SMS has been sent",
                        card.getOwner().getEmail());
                channel.basicAck(deliveryTag, false);
                return;
            }
            // Имитация отправки SMS
            smsService.sendSms(phoneNumber,
                    "A new card has been received: " + card.getCardNumber()
            );

            channel.basicAck(deliveryTag, false);
            log.info("Card '{}' - ack confirmed.", card.getCardNumber());

        } catch (Exception e) {
            log.error("Card processing error", e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}