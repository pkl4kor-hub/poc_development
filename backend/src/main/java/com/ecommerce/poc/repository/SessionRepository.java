package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, String> {
}
