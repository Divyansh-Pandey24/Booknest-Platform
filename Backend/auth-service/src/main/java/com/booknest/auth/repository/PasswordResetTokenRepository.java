package com.booknest.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import com.booknest.auth.entity.PasswordResetToken;
import com.booknest.auth.entity.User;

// Repository for password reset token persistence
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Find a reset token entity by its string value
    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    @Modifying
    void deleteByUserAuthEntity(User user);
    // Modifying is used to indicate that this query modifies the database
    // InvalidDataAccessApiUsageException .
}