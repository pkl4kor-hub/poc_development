package com.ecommerce.microservices.auth.service;

import com.ecommerce.microservices.auth.entity.User;
import com.ecommerce.microservices.auth.entity.UserSession;
import com.ecommerce.microservices.auth.repository.UserRepository;
import com.ecommerce.microservices.auth.repository.UserSessionRepository;
import com.ecommerce.microservices.common.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserSessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        User user = userRepository.findByUsername(username != null ? username : "buyer").orElse(null);
        Long uid = user != null ? user.getId() : 1L;
        UserSession session = new UserSession(sessionId, uid, username != null ? username : "buyer", LocalDateTime.now().plusDays(7));
        sessionRepository.save(session);
        return sessionId;
    }

    public void setSessionUser(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessionRepository.findById(sessionId).ifPresent(s -> {
                s.setUsername(username != null ? username : "buyer");
                userRepository.findByUsername(s.getUsername()).ifPresent(u -> s.setUserId(u.getId()));
                sessionRepository.save(s);
            });
        }
    }

    public String resolveUsername(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionRepository.findById(sessionId)
                    .map(UserSession::getUsername)
                    .orElse("buyer");
        }
        return "buyer";
    }

    @Transactional
    public Optional<LoginResponse> login(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getActive()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return Optional.empty();
        }

        return Optional.of(new LoginResponse(user.getUsername(), user.getRole(), true));
    }

    public Optional<User> getUser(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<UserSession> validateSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()));
    }
}
