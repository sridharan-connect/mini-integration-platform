package com.example.integrationplatform.repository;

import com.example.integrationplatform.entity.BusinessIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessIdempotencyRepository
        extends JpaRepository<BusinessIdempotency, Long> {

    Optional<BusinessIdempotency> findByIdempotencyKey(String idempotencyKey);
}