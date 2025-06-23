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
            log.warn("Registration failed for user: {} - {}", request.getUsername(), e.getMessage());

            // Check if it's a username or email conflict and set the error on the correct
            // field
            if (e.getMessage().toLowerCase().contains("username")) {
                bindingResult.rejectValue("username", "user.exists", e.getMessage());
            } else if (e.getMessage().toLowerCase().contains("email")) {
                bindingResult.rejectValue("email", "email.exists", e.getMessage());
            } else {
                // Generic validation error
                model.addAttribute("error", e.getMessage());
            }
            return "auth/register";
        } catch (Exception e) {
            log.error("Registration failed for user: " + request.getUsername(), e);
            model.addAttribute("error", "Registration failed: " + e.getMessage() + ". Please try again.");
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

    @PostMapping("/validate-password")
    public String validatePassword(@RequestParam("password") String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            Model model) {

        PasswordValidationResult result = validatePasswordStrength(password);

        // Check if passwords match (only if confirmPassword is provided)
        boolean passwordsMatch = true;
        if (confirmPassword != null) {
            passwordsMatch = password.equals(confirmPassword);
        }

        model.addAttribute("password", password);
        model.addAttribute("confirmPassword", confirmPassword);
        model.addAttribute("validationResult", result);
        model.addAttribute("passwordsMatch", passwordsMatch);

        return "fragments/password-validation :: password-validation";
    }

    @PostMapping("/check-username")
    public String checkUsername(@RequestParam("username") String username, Model model) {
        boolean available = true;
        String message = "";

        if (username == null || username.trim().isEmpty()) {
            available = false;
            message = "Username is required";
        } else if (username.length() < 3) {
            available = false;
            message = "Username must be at least 3 characters";
        } else if (username.length() > 50) {
            available = false;
            message = "Username must be less than 50 characters";
        } else if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            available = false;
            message = "Username can only contain letters, numbers, underscores, and hyphens";
        } else if (userService.findByUsername(username.trim()).isPresent()) {
            available = false;
            message = "Username '" + username + "' is already taken";
        } else {
            message = "Username is available";
        }

        model.addAttribute("username", username);
        model.addAttribute("available", available);
        model.addAttribute("message", message);

        return "fragments/username-validation :: username-validation";
    }

    @PostMapping("/check-email")
    public String checkEmail(@RequestParam("email") String email, Model model) {
        boolean available = true;
        String message = "";

        if (email == null || email.trim().isEmpty()) {
            available = false;
            message = "Email is required";
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            available = false;
            message = "Please provide a valid email address";
        } else if (userService.findByEmail(email.trim().toLowerCase()).isPresent()) {
            available = false;
            message = "Email '" + email + "' is already registered";
        } else {
            message = "Email is available";
        }

        model.addAttribute("email", email);
        model.addAttribute("available", available);
        model.addAttribute("message", message);

        return "fragments/email-validation :: email-validation";
    }

    private PasswordValidationResult validatePasswordStrength(String password) {
        PasswordValidationResult result = new PasswordValidationResult();

        if (password == null || password.isEmpty()) {
            result.setValid(false);
            result.setStrength(0);
            result.setMessage("Password is required");
            return result;
        }

        int strength = 0;
        StringBuilder feedback = new StringBuilder();

        // Length check
        if (password.length() >= 8) {
            strength++;
        } else if (password.length() >= 6) {
            feedback.append("Consider using at least 8 characters. ");
        } else {
            result.setValid(false);
            result.setMessage("Password must be at least 6 characters");
            return result;
        }

        // Lowercase letters
        if (password.matches(".*[a-z].*")) {
            strength++;
        } else {
            feedback.append("Add lowercase letters. ");
        }

        // Uppercase letters
        if (password.matches(".*[A-Z].*")) {
            strength++;
        } else {
            feedback.append("Add uppercase letters. ");
        }

        // Numbers
        if (password.matches(".*[0-9].*")) {
            strength++;
        } else {
            feedback.append("Add numbers. ");
        }

        // Special characters
        if (password.matches(".*[^A-Za-z0-9].*")) {
            strength++;
        } else {
            feedback.append("Add special characters. ");
        }

        result.setValid(true);
        result.setStrength(strength);

        if (strength <= 2) {
            result.setStrengthText("Weak");
            result.setStrengthColor("red");
            result.setMessage("Weak password. " + feedback.toString());
        } else if (strength == 3) {
            result.setStrengthText("Fair");
            result.setStrengthColor("orange");
            result.setMessage("Fair password. " + feedback.toString());
        } else if (strength == 4) {
            result.setStrengthText("Good");
            result.setStrengthColor("blue");
            result.setMessage("Good password!");
        } else {
            result.setStrengthText("Strong");
            result.setStrengthColor("green");
            result.setMessage("Strong password!");
        }

        return result;
    }

    public static class PasswordValidationResult {
        private boolean valid;
        private int strength;
        private String strengthText;
        private String strengthColor;
        private String message;

        // Getters and setters
        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public int getStrength() {
            return strength;
        }

        public void setStrength(int strength) {
            this.strength = strength;
        }

        public String getStrengthText() {
            return strengthText;
        }

        public void setStrengthText(String strengthText) {
            this.strengthText = strengthText;
        }

        public String getStrengthColor() {
            return strengthColor;
        }

        public void setStrengthColor(String strengthColor) {
            this.strengthColor = strengthColor;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}