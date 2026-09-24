package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.Role;
import com.ecommerce.poc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByRole(Role role);
}
