package ru.arsentiev.cardconsumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    @Value("${app.sms.mock.host}")
    private String mockHost;

    @Value("${app.sms.mock.port}")
    private int mockPort;

    private final WebClient webClient;

    public void sendSms(String phoneNumber, String text) {
        String url = String.format("http://%s:%d/sms", mockHost, mockPort);

        Map<String, String> payload = new HashMap<>();
        payload.put("phone", phoneNumber);
        payload.put("message", text);

        webClient.post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ SMS sent to {}: {}", phoneNumber, response))
                .doOnError(error -> log.error("❌ Error when sending SMS to {}: {}", phoneNumber, error.getMessage()))
                .subscribe(); // Асинхронный запуск
    }
}
