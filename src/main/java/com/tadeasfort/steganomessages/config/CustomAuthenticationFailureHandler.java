package com.tadeasfort.steganomessages.config;

import com.tadeasfort.steganomessages.model.User;
import com.tadeasfort.steganomessages.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        String errorMessage = "Invalid username or password";

        log.debug("Authentication failed for user: {} with exception: {}", username,
                exception.getClass().getSimpleName());

        if (exception instanceof DisabledException && username != null) {
            // Check if the user exists and if the issue is email verification
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (user.isAccountEnabled() && !user.isEmailVerified()) {
                    errorMessage = "Please verify your email address before signing in. Check your inbox for the verification link.";
                    log.info("Login attempted for unverified user: {}", username);
                } else if (!user.isAccountEnabled()) {
                    errorMessage = "Your account has been disabled. Please contact support.";
                    log.info("Login attempted for disabled user: {}", username);
                }
            }
        }

        // Encode the error message for URL
        String encodedErrorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);

        // Redirect with the specific error message
        String redirectUrl = "/auth/login?error=true&message=" + encodedErrorMessage;

        log.debug("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}