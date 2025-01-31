# Пример взаимодействия микросервисов с использованием RabbitMQ

### Описание

Это приложение состоит из трёх микросервисов, которые взаимодействуют друг с другом через **RabbitMQ**. Каждый микросервис выполняет свою задачу:

1. **Producer** — отправляет сообщения в очередь RabbitMQ.
2. **ItemConsumer** — потребляет сообщения о товарах и отправляет email уведомления владельцам.
3. **CardConsumer** — потребляет сообщения о карточках и **использует мок-сервер** для имитации отправки SMS уведомлений владельцам карточек.

Кроме того, используется **MailHog** для тестирования email уведомлений и **PostgreSQL** для хранения данных.

### Компоненты

#### **Producer**

- **Конфигурация RabbitMQ**:
    - Устанавливается подключение к RabbitMQ.
    - Создаются exchange и очереди (`itemExchange` и `cardExchange`), в которые будут публиковаться сообщения .

- **RabbitProducerService**:
    - Отправляет сообщения о товарах и карточках в соответствующие очереди.

#### **Consumer**

- **Конфигурация RabbitMQ для Consumer**:
    - Настроена обработка сообщений с использованием контейнера для прослушивания очереди с возможностью управления подтверждениями (ack/nack).

- **ItemListener**:
    - Подписывается на очередь товаров и отправляет email уведомления владельцам, используя **EmailNotificationService**.

- **CardListener**:
    - Подписывается на очередь карточек и **использует мок-сервер** для имитации отправки SMS уведомлений владельцам карточек, используя **SmsService**.

#### **Отправка уведомлений**

- **EmailNotificationService**:
    - Отправляет email сообщения владельцам товаров через **JavaMailSender**.

- **SmsService**:
    - **Использует мок-сервер**, который имитирует отправку SMS уведомлений владельцам карточек.

#### **Деплой в Docker**

Для развертывания приложения используется **Docker Compose**, который описывает контейнеры для:

- RabbitMQ
- Producer
- ItemConsumer
- CardConsumer
- MailHog (для тестирования email)
- PostgreSQL

---

### Основной процесс

1. **Producer** генерирует сообщения и отправляет их в RabbitMQ.
2. **ItemConsumer** и **CardConsumer** обрабатывают сообщения из очередей.
3. **ItemConsumer** отправляет email уведомления владельцам товаров.
4. **CardConsumer** **использует мок-сервер** для имитации отправки SMS уведомлений владельцам карточек.
5. **MailHog** позволяет тестировать и проверять отправленные email сообщения.

## Подробное описание с примерами кода

### **Producer**

#### 1. Конфигурация RabbitMQ (RabbitMQConfig)

В классе `RabbitMQConfig` настраиваются все компоненты для подключения к **RabbitMQ**, создание обменников, очередей и биндингов:
- Здесь мы создаём `ConnectionFactory` для подключения к RabbitMQ с параметрами пользователя и пароля.
- Далее настраиваем два типа обменников (`DirectExchange` и `TopicExchange`) для отправки сообщений.
- Создаём очереди (`itemQueue` и `cardQueue`) для разных типов сообщений.
- В биндингах связываем очереди с обменниками через ключи маршрутизации, которые используются для отправки сообщений.
- Создаём `RabbitTemplate`, который используется для отправки сообщений в RabbitMQ, с возможностью подтверждения публикации и обработки возвратов сообщений через механизм callback.

```java
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
  RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // Если сообщение не может быть доставлено по binding (нет очереди или нет rountingKey),
        // оно вернётся в callback
        template.setMandatory(true);

        template.setMessageConverter(messageConverter);

        // Confirm Callback: вызывается, когда broker подтвердил получение (ack) или не подтвердил (nack).
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("ConfirmCallback: Сообщение подтверждено broker'ом! {}",
                        correlationData != null ? correlationData.getId() : "");
            } else {
                log.warn("ConfirmCallback: Сообщение НЕ подтверждено! Причина: {}", cause);
            }
        });

        // Return Callback: вызывается, если сообщение не было маршрутизировано ни в одну очередь.
        template.setReturnsCallback(returned -> {
            log.warn("ReturnCallback: Сообщение возвращено! replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getReplyCode(), returned.getReplyText(), returned.getExchange(), returned.getRoutingKey());
        });

        return template;
  }
```

#### 2. RabbitProducerService

Сервис для отправки сообщений в RabbitMQ. Используется `RabbitTemplate` для отправки объектов типа `Item` и `Card` в соответствующие очереди:
- В сервисе `RabbitProducerService` есть два метода: один для отправки сообщения типа `Item` в очередь `itemQueue`, а второй — для отправки сообщения типа `Card` в очередь `cardQueue`.
- Методы используют `RabbitTemplate` для отправки сообщений, которые автоматически преобразуются в формат, подходящий для RabbitMQ.

```java
    public void sendItem(Item item) {
        // Можно добавлять correlationData (ID), чтобы отслеживать в confirm callback
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
```
---

### **Consumer**

#### 1. Общая конфигурация RabbitMQ для 2-х Consumer

В классе `RabbitConfig` настраиваем контейнеры слушателей для обработки сообщений:

```java
@Configuration
public class RabbitConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(3);
        factory.setPrefetchCount(3);
        return factory;
    }
}
```

**Краткое описание:**
- Создаём конфигурацию для слушателей RabbitMQ.
- Устанавливаем режим подтверждения сообщений вручную (для более точного контроля за состоянием сообщения).
- Настроили использование нескольких потребителей для одновременной обработки сообщений и ограничили количество сообщений на один потребитель (через `prefetchCount`).

#### 2. ItemListener (Обработка сообщений для Item)

Слушатель для обработки сообщений, которые приходят в очередь `itemQueue`:

```java
@Service
public class ItemListener {

    private final EmailNotificationService emailNotificationService;

    public ItemListener(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @RabbitListener(queues = "itemQueue")
    public void handleItem(Item item, Channel channel, Message message) {
        try {
            String email = item.getOwner().getEmail();
            emailNotificationService.sendEmail(email, "Скидка на товар", "Товар " + item.getName() + " имеет скидку!");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
```

**Краткое описание:**
- Слушатель с аннотацией `@RabbitListener` прослушивает очередь `itemQueue` для сообщений типа `Item`.
- При получении сообщения выполняется отправка email-уведомления через сервис `EmailNotificationService`.
- Если сообщение обработано успешно, оно подтверждается с помощью метода `basicAck`. В случае ошибки — сообщение отклоняется с помощью `basicNack` для повторной обработки.

#### 3. CardListener (Обработка сообщений для Card)

Слушатель для обработки сообщений из очереди `cardQueue`:

```java
@Service
public class CardListener {

    private final SmsService smsService;

    public CardListener(SmsService smsService) {
        this.smsService = smsService;
    }

    @RabbitListener(queues = "cardQueue")
    public void handleCard(Card card, Channel channel, Message message) {
        try {
            String phoneNumber = card.getOwner().getPhoneNumber();
            if (phoneNumber != null) {
                smsService.sendSms(phoneNumber, "Уведомление о карточке " + card.getName());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
```

**Краткое описание:**
- Слушатель для очереди `cardQueue`, который обрабатывает сообщения типа `Card`.
- Когда сообщение приходит, выполняется отправка SMS через сервис `SmsService`.
- Сообщение подтверждается или отклоняется в зависимости от результата обработки.

---

### **Отправка уведомлений**

#### 1. EmailNotificationService (Отправка email)

Сервис для отправки email уведомлений:
- Сервис использует `JavaMailSender` для отправки простых email-сообщений. Этот сервис используется в `ItemListener` для отправки уведомлений пользователям.

Вот обновлённое описание для **SmsService**:

---

#### **2. SmsService (Использование мок-сервера для имитации отправки SMS)**

Сервис для имитации отправки SMS-сообщений через внешний сервис с использованием мок-сервера:
- Вместо реальной отправки SMS сообщений, используется **мок-сервер**, который эмулирует процесс отправки.
- Сервис использует асинхронный подход для взаимодействия с внешним API через **WebClient**.
- Все запросы обрабатываются асинхронно, не блокируя основной поток выполнения приложения.