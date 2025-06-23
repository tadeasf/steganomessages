package com.tadeasfort.steganomessages.controller;

import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String profile(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "profile/index";
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "profile/settings";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal User user,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(user, firstName, lastName);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (Exception e) {
            log.error("Failed to update profile for user: {}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update profile");
        }
        return "redirect:/profile/settings";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal User user,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match");
                return "redirect:/profile/settings";
            }

            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters long");
                return "redirect:/profile/settings";
            }

            userService.changePassword(user, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to change password for user: {}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to change password");
        }
        return "redirect:/profile/settings";
    }

    @PostMapping("/request-password-reset")
    public String requestPasswordReset(@AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            userService.initiatePasswordReset(user.getEmail());
            redirectAttributes.addFlashAttribute("success",
                    "Password reset instructions sent to your email address");
        } catch (Exception e) {
            log.error("Failed to initiate password reset for user: {}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to send password reset email");
        }
        return "redirect:/profile/settings";
    }
}