package com.boardify.boardify_service.auth.repository;

import com.boardify.boardify_service.auth.entity.PasswordResetToken;
import com.boardify.boardify_service.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(UserEntity user); // To clean up old tokens
}
