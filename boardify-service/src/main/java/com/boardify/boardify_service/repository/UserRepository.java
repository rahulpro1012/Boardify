package com.boardify.boardify_service.repository;

import java.util.Optional;
import java.util.UUID;

import com.boardify.boardify_service.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}