package com.tadeasfort.steganomessages.repository;

import com.tadeasfort.steganomessages.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void deleteByVerificationTokenExpiresAtBefore(LocalDateTime dateTime);

    void deleteByPasswordResetTokenExpiresAtBefore(LocalDateTime dateTime);
}