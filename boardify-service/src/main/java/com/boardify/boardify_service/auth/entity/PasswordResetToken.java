package com.boardify.boardify_service.auth.entity;


import com.boardify.boardify_service.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken() {}

    public PasswordResetToken(UserEntity user) {
        this.user = user;
        this.token = UUID.randomUUID().toString(); // Generate random token
        this.expiryDate = LocalDateTime.now().plusMinutes(15); // Expires in 15 mins
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    // Getters and Setters...
}