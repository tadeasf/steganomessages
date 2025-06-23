package com.tadeasfort.steganomessages.repository;

import com.tadeasfort.steganomessages.model.SteganographyMessage;
import com.tadeasfort.steganomessages.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SteganographyMessageRepository extends JpaRepository<SteganographyMessage, Long> {

    Page<SteganographyMessage> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<SteganographyMessage> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

    Optional<SteganographyMessage> findByShareToken(String shareToken);

    List<SteganographyMessage> findByExpiresAtBefore(LocalDateTime dateTime);

    @Query("SELECT COUNT(sm) FROM SteganographyMessage sm WHERE sm.user = :user")
    long countByUser(User user);

    @Query("SELECT SUM(sm.downloadCount) FROM SteganographyMessage sm WHERE sm.user = :user")
    Long getTotalDownloadsByUser(User user);

    @Modifying
    @Query("DELETE FROM SteganographyMessage sm WHERE sm.expiresAt < :dateTime")
    void deleteExpiredMessages(LocalDateTime dateTime);
}