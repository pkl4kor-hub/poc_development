package com.ecommerce.poc.controller;

import com.ecommerce.poc.dto.LoginRequest;
import com.ecommerce.poc.dto.LoginResponse;
import com.ecommerce.poc.service.AuthService;
import com.ecommerce.poc.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionService sessionService;

    public AuthController(AuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        if (sessionId != null && !sessionId.isBlank()) {
            sessionService.setSessionUser(sessionId, response.getUsername());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        authService.logout();
        if (sessionId != null && !sessionId.isBlank()) {
            sessionService.setSessionUser(sessionId, "buyer");
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String username = null;
        if (sessionId != null && !sessionId.isBlank()) {
            username = sessionService.resolveUsername(sessionId);
        }
        if (username != null) {
            var userOpt = authService.getUser(username);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(new LoginResponse(userOpt.get().getUsername(), userOpt.get().getRole().name(), true));
            }
        }
        return ResponseEntity.ok(authService.me());
    }
}
