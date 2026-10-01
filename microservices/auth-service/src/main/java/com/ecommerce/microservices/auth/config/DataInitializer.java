package com.ecommerce.microservices.auth.config;

import com.ecommerce.microservices.auth.entity.User;
import com.ecommerce.microservices.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User buyer = new User();
                buyer.setUsername("buyer");
                buyer.setPasswordHash(passwordEncoder.encode("Buyer@123"));
                buyer.setRole("BUYER");
                buyer.setActive(true);
                userRepository.save(buyer);

                User seller = new User();
                seller.setUsername("seller");
                seller.setPasswordHash(passwordEncoder.encode("Seller@123"));
                seller.setRole("SELLER");
                seller.setActive(true);
                userRepository.save(seller);

                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setRole("ADMIN");
                admin.setActive(true);
                userRepository.save(admin);
            }
        };
    }
}
