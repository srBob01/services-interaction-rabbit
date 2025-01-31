package ru.arsentiev.itemconsumer.model;

import lombok.Data;

@Data
public class Item {
    private Long id;
    private String name;
    private Owner owner; // вложенный объект

    @Data
    public static class Owner {
        private String email;
        private String name;
        private String phoneNumber;
    }
}
