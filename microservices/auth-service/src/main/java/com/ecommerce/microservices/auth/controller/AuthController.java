package com.ecommerce.microservices.auth.controller;

import com.ecommerce.microservices.auth.service.AuthService;
import com.ecommerce.microservices.common.dto.LoginRequest;
import com.ecommerce.microservices.common.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> createSession() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("sessionId", authService.createSession("buyer")));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword())
                .map(res -> {
                    if (sessionId != null && !sessionId.isBlank()) {
                        authService.setSessionUser(sessionId, res.getUsername());
                    }
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            authService.setSessionUser(sessionId, "buyer");
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<LoginResponse> me(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String username = authService.resolveUsername(sessionId);
        var userOpt = authService.getUser(username);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(new LoginResponse(userOpt.get().getUsername(), userOpt.get().getRole(), true));
        }
        return ResponseEntity.ok(new LoginResponse("buyer", "BUYER", true));
    }
}
