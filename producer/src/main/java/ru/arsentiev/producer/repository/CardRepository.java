package ru.arsentiev.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arsentiev.producer.entity.Card;


public interface CardRepository extends JpaRepository<Card, Long> {
}
