package ru.arsentiev.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arsentiev.producer.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
