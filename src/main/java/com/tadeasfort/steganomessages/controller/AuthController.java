package com.tadeasfort.steganomessages.controller;

import com.tadeasfort.steganomessages.dto.LoginRequest;
import com.tadeasfort.steganomessages.dto.RegistrationRequest;
import com.tadeasfort.steganomessages.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @Lazy
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "message", required = false) String customMessage,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            String errorMessage = "Invalid username or password";
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                errorMessage = customMessage;
            }
            model.addAttribute("error", errorMessage);
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("currentPage", "login");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        model.addAttribute("currentPage", "register");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
            return "auth/register";
        }

        try {
            userService.createUser(request.getUsername(), request.getEmail(),
                    request.getPassword(), request.getFirstName(), request.getLastName());

            redirectAttributes.addFlashAttribute("message",
                    "Registration successful! Please check your email to verify your account.");
            return "redirect:/auth/login";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", "user.exists", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            log.error("Registration failed for user: " + request.getUsername(), e);
            model.addAttribute("error", "Registration failed. Please try again.");
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token,
            RedirectAttributes redirectAttributes) {

        boolean verified = userService.verifyEmail(token);

        if (verified) {
            redirectAttributes.addFlashAttribute("message",
                    "Email verified successfully! You can now log in.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Invalid or expired verification token.");
        }

        return "redirect:/auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        userService.initiatePasswordReset(email);
        redirectAttributes.addFlashAttribute("message",
                "If an account with that email exists, we've sent you a password reset link.");

        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token,
            @RequestParam("password") String password,
            Model model) {
        try {
            boolean success = userService.resetPassword(token, password);
            if (success) {
                model.addAttribute("success", "Password reset successfully");
                return "auth/login";
            } else {
                model.addAttribute("error", "Invalid or expired password reset token");
                model.addAttribute("token", token);
                return "auth/reset-password";
            }
        } catch (Exception e) {
            log.error("Password reset failed for token: {}", token, e);
            model.addAttribute("error", "Password reset failed");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }

    @PostMapping("/request-password-reset")
    public String requestPasswordReset(@RequestParam("email") String email, Model model) {
        try {
            userService.initiatePasswordReset(email);
            model.addAttribute("success", "Password reset instructions sent to your email");
            return "auth/login";
        } catch (Exception e) {
            log.error("Password reset request failed for email: {}", email, e);
            model.addAttribute("error", "If the email exists, password reset instructions will be sent");
            return "auth/login";
        }
    }

    @GetMapping("/resend-verification")
    public String resendVerificationPage() {
        return "auth/resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        userService.resendVerificationEmail(email);
        redirectAttributes.addFlashAttribute("message",
                "If an unverified account with that email exists, we've sent you a new verification link.");

        return "redirect:/auth/resend-verification";
    }
}