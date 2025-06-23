package com.tadeasfort.steganomessages.controller;

import com.tadeasfort.steganomessages.service.SteganographyMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.model.SteganographyMessage;
import com.tadeasfort.steganomessages.service.UserStatisticsService;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final SteganographyMessageService messageService;
    private final UserStatisticsService statisticsService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/home")
    public String homeAlternative() {
        return "redirect:/";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/decode")
    public String decode() {
        return "decode";
    }

    // Keep the old extract route for backwards compatibility
    @GetMapping("/extract")
    public String extract() {
        return "redirect:/decode";
    }

    /**
     * Share endpoint for public messages
     */
    @GetMapping("/share/{shareToken}")
    public String shareMessage(@PathVariable String shareToken, Model model) {
        Optional<SteganographyMessage> messageOpt = messageService.findByShareToken(shareToken);

        if (messageOpt.isEmpty()) {
            model.addAttribute("error", "Shared message not found or has expired");
            return "error";
        }

        SteganographyMessage message = messageOpt.get();

        // Check if message is public
        if (!message.isPublic()) {
            model.addAttribute("error", "This message is not publicly accessible");
            return "error";
        }

        // Check if message has expired
        if (message.getExpiresAt() != null && message.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            model.addAttribute("error", "This shared message has expired");
            return "error";
        }

        model.addAttribute("message", message);
        model.addAttribute("shareUrl", "/share/" + shareToken);
        model.addAttribute("downloadUrl", "/download/shared/" + shareToken);

        return "share-message";
    }

    /**
     * Download endpoint for steganography messages
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadSteganographyMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        try {
            SteganographyMessage message = messageService.getMessageForUser(id, user);
            Path filePath = Path.of(message.getFilePath());

            if (!Files.exists(filePath)) {
                log.error("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            // Increment download count and record statistics
            messageService.incrementDownloadCount(message);
            if (user != null) {
                statisticsService.recordDownload(user);
            }

            // Determine content type
            String contentType = message.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Create response with proper headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + message.getStegoFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to download steganography message with ID: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Public download endpoint for shared messages - allows anonymous downloads of
     * public messages
     */
    @GetMapping("/download/shared/{shareToken}")
    public ResponseEntity<Resource> downloadSharedMessage(@PathVariable String shareToken) {
        try {
            Optional<SteganographyMessage> messageOpt = messageService.findByShareToken(shareToken);

            if (messageOpt.isEmpty()) {
                log.warn("Shared message not found for token: {}", shareToken);
                return ResponseEntity.notFound().build();
            }

            SteganographyMessage message = messageOpt.get();

            // Check if message is public
            if (!message.isPublic()) {
                log.warn("Attempted to download non-public shared message: {}", shareToken);
                return ResponseEntity.notFound().build();
            }

            // Check if message has expired
            if (message.getExpiresAt() != null && message.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                log.warn("Attempted to download expired shared message: {}", shareToken);
                return ResponseEntity.notFound().build();
            }

            Path filePath = Path.of(message.getFilePath());

            if (!Files.exists(filePath)) {
                log.error("Shared message file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            // Increment download count (no user statistics for anonymous downloads)
            messageService.incrementDownloadCount(message);

            // Determine content type
            String contentType = message.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Public download of shared message: {} (token: {})", message.getTitle(), shareToken);

            // Create response with proper headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + message.getStegoFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to download shared message with token: {}", shareToken, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/extract")
    public String extractMessage(@RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal User user, Model model) {
        return decodeMessage(imageFile, user, model);
    }

    @PostMapping("/api/decode")
    public String decodeMessage(@RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal User user, Model model) {
        try {
            String decodedMessage;
            if (user != null) {
                // Use method that tracks statistics for authenticated users
                decodedMessage = messageService.extractMessageWithUser(imageFile, user);
            } else {
                // Use regular method for anonymous users
                decodedMessage = messageService.extractMessage(imageFile);
            }

            model.addAttribute("decodedMessage", decodedMessage);
            model.addAttribute("success", true);
            return "fragments/decode-result :: decode-result";
        } catch (Exception e) {
            log.error("Failed to decode message from image", e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", false);
            return "fragments/decode-result :: decode-result";
        }
    }

    @PostMapping("/api/create-message")
    public String createMessage(@RequestParam("title") String title,
            @RequestParam("message") String message,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("isPublic") boolean isPublic,
            @AuthenticationPrincipal User user,
            Model model) {
        return encodeMessage(title, message, imageFile, password, isPublic, user, model);
    }

    @PostMapping("/api/encode")
    public String encodeMessage(@RequestParam("title") String title,
            @RequestParam("message") String message,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("isPublic") boolean isPublic,
            @AuthenticationPrincipal User user,
            Model model) {
        try {
            // Call with correct parameter order: user, title, message, imageFile, isPublic,
            // password, expirationDays
            var stegoMessage = messageService.createMessage(user, title, message, imageFile, isPublic, password, null);

            model.addAttribute("success", true);
            model.addAttribute("message", stegoMessage);
            model.addAttribute("downloadUrl", "/download/" + stegoMessage.getId());
            return "fragments/encode-result :: encode-result";
        } catch (Exception e) {
            log.error("Failed to encode steganographic message", e);
            model.addAttribute("success", false);
            model.addAttribute("error", e.getMessage());
            return "fragments/encode-result :: encode-result";
        }
    }

    @GetMapping("/public")
    public String publicMessages(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var messages = messageService.getPublicMessages(PageRequest.of(page, size));
        model.addAttribute("messages", messages);
        model.addAttribute("currentPage", page);
        return "public-messages";
    }
}