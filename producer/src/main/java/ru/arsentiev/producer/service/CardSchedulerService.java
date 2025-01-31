package ru.arsentiev.producer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.arsentiev.producer.repository.CardRepository;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSchedulerService {

    private final CardRepository cardRepository;
    private final RabbitProducerService rabbitProducerService;
    private final Random random = new Random();

    // Раз в 15 секунд, но, с начальной задержкой 10 секунд
    @Scheduled(fixedRate = 15000, initialDelay = 10000)
    public void publishRandomCard() {
        long count = cardRepository.count();
        if (count == 0) {
            log.info("Нет Cards в БД");
            return;
        }
        long randomId = random.nextLong(count) + 1;
        cardRepository.findById(randomId).ifPresent(card -> {
            rabbitProducerService.sendCard(card);
            log.info("Опубликовали Card: {}", card.getCardNumber());
        });
    }
}
