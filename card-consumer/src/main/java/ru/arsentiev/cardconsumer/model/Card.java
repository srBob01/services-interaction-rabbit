package ru.arsentiev.cardconsumer.model;

import lombok.Data;

@Data
public class Card {
    private Long id;
    private String cardNumber;
    private Owner owner;

    @Data
    public static class Owner {
        private String email;
        private String name;
        private String phoneNumber;
    }
}