package com.tadeasfort.steganomessages.service;

import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.model.UserStatistics;
import com.tadeasfort.steganomessages.repository.UserStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserStatisticsService {

    private final UserStatisticsRepository statisticsRepository;

    public UserStatistics getOrCreateUserStatistics(User user) {
        return statisticsRepository.findByUser(user)
                .orElseGet(() -> createDefaultStatistics(user));
    }

    private UserStatistics createDefaultStatistics(User user) {
        UserStatistics stats = new UserStatistics();
        stats.setUser(user);
        stats.setMessagesCreated(0L);
        stats.setMessagesExtracted(0L);
        stats.setCharactersEmbedded(0L);
        stats.setCharactersExtracted(0L);
        stats.setTotalDownloads(0L);
        stats.setImagesProcessed(0L);

        return statisticsRepository.save(stats);
    }

    public void recordMessageCreated(User user, String message) {
        UserStatistics stats = getOrCreateUserStatistics(user);
        stats.incrementMessagesCreated(message.length());
        statisticsRepository.save(stats);

        log.info("Recorded message creation for user: {} - message length: {}",
                user.getUsername(), message.length());
    }

    public void recordMessageExtracted(User user, String extractedMessage) {
        UserStatistics stats = getOrCreateUserStatistics(user);
        stats.incrementMessagesExtracted(extractedMessage.length());
        statisticsRepository.save(stats);

        log.info("Recorded message extraction for user: {} - message length: {}",
                user.getUsername(), extractedMessage.length());
    }

    public void recordDownload(User user) {
        UserStatistics stats = getOrCreateUserStatistics(user);
        stats.incrementDownloads();
        statisticsRepository.save(stats);

        log.info("Recorded download for user: {}", user.getUsername());
    }

    public UserStatistics getUserStatistics(User user) {
        return getOrCreateUserStatistics(user);
    }
}