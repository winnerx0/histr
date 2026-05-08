package com.histr.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Table(name = "refresh_token")
@Entity
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String tokenHash;

    private Instant expiresAt;

    private boolean blacklisted;

    private Instant createdAt = Instant.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}
