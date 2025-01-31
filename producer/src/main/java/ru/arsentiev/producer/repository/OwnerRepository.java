package ru.arsentiev.producer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arsentiev.producer.entity.Owner;

public interface OwnerRepository extends JpaRepository<Owner, String> {
}