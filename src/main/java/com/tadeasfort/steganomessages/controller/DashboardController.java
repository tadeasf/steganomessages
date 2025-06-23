package com.tadeasfort.steganomessages.controller;

import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.model.UserStatistics;
import com.tadeasfort.steganomessages.service.SteganographyMessageService;
import com.tadeasfort.steganomessages.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserStatisticsService statisticsService;
    private final SteganographyMessageService messageService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        // Get user statistics
        UserStatistics stats = statisticsService.getUserStatistics(user);
        model.addAttribute("statistics", stats);

        // Get recent messages (last 5)
        var recentMessages = messageService.getUserMessages(user, PageRequest.of(0, 5));
        model.addAttribute("recentMessages", recentMessages.getContent());

        // Add user info
        model.addAttribute("user", user);

        log.info("Dashboard accessed by user: {}", user.getUsername());
        return "dashboard/index";
    }

    @GetMapping("/encode")
    public String encodePage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "dashboard/encode";
    }

    @GetMapping("/decode")
    public String decodePage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "dashboard/decode";
    }

    @GetMapping("/messages")
    public String messagesPage(@AuthenticationPrincipal User user, Model model) {
        var messages = messageService.getUserMessages(user, PageRequest.of(0, 20));
        model.addAttribute("messages", messages.getContent());
        model.addAttribute("user", user);
        return "dashboard/messages";
    }

    @GetMapping("/statistics")
    public String statisticsPage(@AuthenticationPrincipal User user, Model model) {
        UserStatistics stats = statisticsService.getUserStatistics(user);
        model.addAttribute("statistics", stats);
        model.addAttribute("user", user);
        return "dashboard/statistics";
    }

    @PostMapping("/messages/{id}/delete")
    public String deleteMessage(@PathVariable Long id, @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            messageService.deleteMessage(id, user);
            redirectAttributes.addFlashAttribute("success", "Message deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete message with ID: {} for user: {}", id, user.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete message. Please try again.");
        }
        return "redirect:/dashboard/messages";
    }
}