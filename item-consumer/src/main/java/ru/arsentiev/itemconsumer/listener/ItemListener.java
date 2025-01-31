package ru.arsentiev.itemconsumer.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.arsentiev.itemconsumer.model.Item;
import ru.arsentiev.itemconsumer.service.EmailNotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemListener {

    private final EmailNotificationService emailService;

    /**
     * Слушаем очередь itemQueue.
     * containerFactory = "rabbitListenerContainerFactory" => manual ack, concurrency=3 и т.д.
     */
    @RabbitListener(queues = "${app.rabbit.item.queue}", containerFactory = "rabbitListenerContainerFactory")
    public void receiveItem(Item item,
                            Message message,
                            Channel channel) throws Exception {
        // deliveryTag нужен для ручного ack
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("Получено сообщение об Item: {}", item);

            // Отправляем email (MailHog)
            emailService.sendEmail(
                    item.getOwner().getEmail(),
                    "Discount on " + item.getName(),
                    "Owner " + item.getOwner().getName() + ", there is a discount for the product "
                    + item.getName() + "\"!"
            );

            // Если всё ок, подтверждаем
            channel.basicAck(deliveryTag, false);
            log.info("Item '{}' - ack confirmed.", item.getName());

        } catch (Exception e) {
            log.error("Ошибка при обработке Item", e);

            // Отклоняем без повторной отправки (requeue=false)
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
