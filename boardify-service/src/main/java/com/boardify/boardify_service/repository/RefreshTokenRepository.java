package com.boardify.boardify_service.repository;

import com.boardify.boardify_service.auth.entity.RefreshToken;
import com.boardify.boardify_service.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    Optional<RefreshToken> findByTokenAndUser(String token, UserEntity user);

    void deleteByToken(String token);

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);
}

