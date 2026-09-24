package com.ecommerce.poc.service;

import com.ecommerce.poc.entity.Session;
import com.ecommerce.poc.repository.SessionRepository;
import com.ecommerce.poc.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    public String createSession() {
        return createSession("buyer");
    }

    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessionRepository.save(new Session(sessionId, Instant.now().toString(), username != null ? username : "buyer"));
        return sessionId;
    }

    public boolean isValidSession(String sessionId) {
        return sessionId != null && !sessionId.isBlank() && sessionRepository.existsById(sessionId);
    }

    public void setSessionUser(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessionRepository.findById(sessionId).ifPresent(s -> {
                s.setUsername(username != null ? username : "buyer");
                sessionRepository.save(s);
            });
        }
    }

    public String resolveUsername(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionRepository.findById(sessionId)
                    .map(s -> s.getUsername() != null && !s.getUsername().isBlank() ? s.getUsername() : "buyer")
                    .orElse(null);
        }
        return null;
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessionRepository.deleteById(sessionId);
        }
    }
}
