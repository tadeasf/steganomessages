package com.tadeasfort.steganomessages.service;

import com.tadeasfort.steganomessages.model.SteganographyMessage;
import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.repository.SteganographyMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SteganographyMessageService {

    private final SteganographyMessageRepository messageRepository;
    private final DCTSteganographyService dctSteganographyService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final UserStatisticsService statisticsService;

    public SteganographyMessage createMessage(User user, String title, String message,
            MultipartFile imageFile, boolean isPublic,
            String sharePassword, Integer expirationDays) throws IOException {

        // Validate image file
        fileStorageService.validateImageFile(imageFile);

        // Load the image
        BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());

        // Check if message can fit in the image
        int maxLength = dctSteganographyService.getMaxMessageLength(originalImage);
        if (message.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Message too long for this image. Maximum length: " + maxLength + " characters");
        }

        // Embed the message
        BufferedImage stegoImage = dctSteganographyService.embedMessage(originalImage, message);

        // Generate unique filename for stego image
        String originalFilename = imageFile.getOriginalFilename();
        String stegoFilename = UUID.randomUUID().toString() + ".png"; // Always save as PNG to avoid JPEG compression

        // Save the stego image
        String stegoFilePath = fileStorageService.getFilePath(stegoFilename).toString();
        dctSteganographyService.saveImage(stegoImage, "png", stegoFilePath);

        // Create and save the message record
        SteganographyMessage stegoMessage = new SteganographyMessage();
        stegoMessage.setUser(user);
        stegoMessage.setTitle(title);
        stegoMessage.setMessage(message);
        stegoMessage.setOriginalFilename(originalFilename);
        stegoMessage.setStegoFilename(stegoFilename);
        stegoMessage.setFilePath(stegoFilePath);
        stegoMessage.setFileSize(imageFile.getSize());
        stegoMessage.setMimeType("image/png");
        stegoMessage.setPublic(isPublic);
        stegoMessage.setShareToken(UUID.randomUUID().toString());

        if (sharePassword != null && !sharePassword.trim().isEmpty()) {
            stegoMessage.setPasswordProtected(true);
            stegoMessage.setSharePassword(passwordEncoder.encode(sharePassword));
        }

        if (expirationDays != null && expirationDays > 0) {
            stegoMessage.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        }

        stegoMessage = messageRepository.save(stegoMessage);

        // Record statistics
        statisticsService.recordMessageCreated(user, message);

        log.info("Created steganography message with ID: {} for user: {}", stegoMessage.getId(), user.getUsername());

        return stegoMessage;
    }

    public String extractMessage(MultipartFile imageFile) throws IOException {
        fileStorageService.validateImageFile(imageFile);

        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        String extractedMessage = dctSteganographyService.extractMessage(image);

        log.info("Successfully extracted message from uploaded image");
        return extractedMessage;
    }

    public String extractMessageWithUser(MultipartFile imageFile, User user) throws IOException {
        String extractedMessage = extractMessage(imageFile);

        // Record statistics for authenticated user
        if (user != null) {
            statisticsService.recordMessageExtracted(user, extractedMessage);
        }

        return extractedMessage;
    }

    public Page<SteganographyMessage> getUserMessages(User user, Pageable pageable) {
        return messageRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public Page<SteganographyMessage> getPublicMessages(Pageable pageable) {
        return messageRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);
    }

    public Optional<SteganographyMessage> findByShareToken(String shareToken) {
        return messageRepository.findByShareToken(shareToken);
    }

    public Optional<SteganographyMessage> findById(Long id) {
        return messageRepository.findById(id);
    }

    public SteganographyMessage getMessageForUser(Long id, User user) {
        Optional<SteganographyMessage> messageOpt = messageRepository.findById(id);

        if (messageOpt.isEmpty()) {
            throw new IllegalArgumentException("Message not found");
        }

        SteganographyMessage message = messageOpt.get();

        if (!message.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }

        return message;
    }

    public boolean validateSharePassword(SteganographyMessage message, String password) {
        if (!message.isPasswordProtected()) {
            return true;
        }

        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        return passwordEncoder.matches(password, message.getSharePassword());
    }

    public void incrementDownloadCount(SteganographyMessage message) {
        message.incrementDownloadCount();
        messageRepository.save(message);
    }

    public void deleteMessage(Long id, User user) {
        SteganographyMessage message = getMessageForUser(id, user);

        // Delete the file
        fileStorageService.deleteFile(message.getStegoFilename());

        // Delete the database record
        messageRepository.delete(message);

        log.info("Deleted steganography message with ID: {} for user: {}", id, user.getUsername());
    }

    public void updateMessage(Long id, User user, String title, boolean isPublic,
            String sharePassword, Integer expirationDays) {
        SteganographyMessage message = getMessageForUser(id, user);

        message.setTitle(title);
        message.setPublic(isPublic);

        if (sharePassword != null && !sharePassword.trim().isEmpty()) {
            message.setPasswordProtected(true);
            message.setSharePassword(passwordEncoder.encode(sharePassword));
        } else {
            message.setPasswordProtected(false);
            message.setSharePassword(null);
        }

        if (expirationDays != null && expirationDays > 0) {
            message.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        } else {
            message.setExpiresAt(null);
        }

        messageRepository.save(message);
        log.info("Updated steganography message with ID: {} for user: {}", id, user.getUsername());
    }

    public long getUserMessageCount(User user) {
        return messageRepository.countByUser(user);
    }

    public Long getUserTotalDownloads(User user) {
        Long downloads = messageRepository.getTotalDownloadsByUser(user);
        return downloads != null ? downloads : 0L;
    }

    @Transactional
    public void cleanupExpiredMessages() {
        LocalDateTime now = LocalDateTime.now();
        var expiredMessages = messageRepository.findByExpiresAtBefore(now);

        for (SteganographyMessage message : expiredMessages) {
            fileStorageService.deleteFile(message.getStegoFilename());
        }

        messageRepository.deleteExpiredMessages(now);
        log.info("Cleaned up {} expired messages", expiredMessages.size());
    }
}