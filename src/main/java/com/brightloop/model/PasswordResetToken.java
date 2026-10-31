package com.brightloop.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String token;

    @ManyToOne(optional = false)
    public AppUser user;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    public LocalDateTime expiresAt;

    public LocalDateTime usedAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return usedAt == null && !isExpired();
    }
}
