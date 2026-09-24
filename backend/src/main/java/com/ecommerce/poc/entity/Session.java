package com.ecommerce.poc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "created_at")
    private String createdAt = Instant.now().toString();

    @Column(name = "username")
    private String username = "buyer";

    public Session() {}

    public Session(String id, String createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.username = "buyer";
    }

    public Session(String id, String createdAt, String username) {
        this.id = id;
        this.createdAt = createdAt;
        this.username = username != null ? username : "buyer";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
