package com.tadeasfort.steganomessages.controller;

import com.tadeasfort.steganomessages.model.SteganographyMessage;
import com.tadeasfort.steganomessages.service.SteganographyMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@Controller
@RequestMapping("/fragments")
@RequiredArgsConstructor
public class FragmentsController {

    private final SteganographyMessageService messageService;

    /**
     * Serves the user dropdown menu fragment
     */
    @GetMapping("/user-menu")
    public String getUserMenu() {
        return "fragments/user-menu :: user-menu";
    }

    /**
     * Serves the closed user dropdown menu fragment
     */
    @GetMapping("/user-menu-closed")
    public String getUserMenuClosed() {
        return "fragments/user-menu :: user-menu-closed";
    }

    /**
     * Serves the mobile menu fragment
     */
    @GetMapping("/mobile-menu")
    public String getMobileMenu(Model model, HttpServletRequest request) {
        // Add current page context for mobile menu
        String currentPath = request.getRequestURI();
        if (currentPath.contains("/auth/login")) {
            model.addAttribute("currentPage", "login");
        } else if (currentPath.contains("/auth/register")) {
            model.addAttribute("currentPage", "register");
        }

        return "fragments/mobile-menu :: mobile-menu";
    }

    /**
     * Serves the closed mobile menu fragment
     */
    @GetMapping("/mobile-menu-closed")
    public String getMobileMenuClosed() {
        return "fragments/mobile-menu :: mobile-menu-closed";
    }

    /**
     * Serves the share modal fragment for steganography messages
     */
    @GetMapping("/share-modal/{shareToken}")
    public String getShareModal(@PathVariable String shareToken, Model model, HttpServletRequest request) {
        Optional<SteganographyMessage> messageOpt = messageService.findByShareToken(shareToken);

        if (messageOpt.isEmpty()) {
            model.addAttribute("error", "Message not found");
            return "fragments/share-modal :: share-modal";
        }

        SteganographyMessage message = messageOpt.get();

        if (!message.isPublic()) {
            model.addAttribute("error", "This message is not publicly shareable");
            return "fragments/share-modal :: share-modal";
        }

        // Build the full share URL
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        String shareUrl = baseUrl + "/share/" + shareToken;

        model.addAttribute("message", message);
        model.addAttribute("shareUrl", shareUrl);

        return "fragments/share-modal :: share-modal";
    }
}