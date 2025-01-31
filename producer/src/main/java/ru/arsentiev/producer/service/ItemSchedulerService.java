package ru.arsentiev.producer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.arsentiev.producer.repository.ItemRepository;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemSchedulerService {

    private final ItemRepository itemRepository;
    private final RabbitProducerService rabbitProducerService;
    private final Random random = new Random();

    // Раз в 15 секунд
    @Scheduled(fixedRate = 15000)
    public void publishRandomItem() {
        long count = itemRepository.count();
        if (count == 0) {
            log.info("Нет Items в БД");
            return;
        }
        // Берём случайный ID
        long randomId = random.nextLong(count) + 1;
        itemRepository.findById(randomId).ifPresent(item -> {
            rabbitProducerService.sendItem(item);
            log.info("Опубликовали Item: {}", item.getName());
        });
    }
}