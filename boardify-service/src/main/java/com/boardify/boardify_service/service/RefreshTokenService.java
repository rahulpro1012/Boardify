package com.boardify.boardify_service.service;

import com.boardify.boardify_service.auth.entity.RefreshToken;
import com.boardify.boardify_service.repository.RefreshTokenRepository;
import com.boardify.boardify_service.repository.UserRepository;
import com.boardify.boardify_service.user.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(UserEntity user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS)) // 7 days example
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> rt.getExpiryDate().isAfter(Instant.now())); // check expiry
    }


    public void revokeUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // Delete old token
        refreshTokenRepository.delete(oldToken);

        // Create and return a new one
        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}

