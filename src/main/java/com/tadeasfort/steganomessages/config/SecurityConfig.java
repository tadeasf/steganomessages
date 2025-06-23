package com.tadeasfort.steganomessages.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.DefaultSecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomAuthenticationFailureHandler customFailureHandler;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public SimpleUrlAuthenticationSuccessHandler successHandler() {
                SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
                handler.setDefaultTargetUrl("/dashboard");
                handler.setAlwaysUseDefaultTargetUrl(false);
                return handler;
        }

        @Bean
        public DefaultSecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authz -> authz
                                                // Public endpoints
                                                .requestMatchers("/", "/home", "/about").permitAll()
                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers("/share/**").permitAll()
                                                .requestMatchers("/decode", "/extract").permitAll()
                                                .requestMatchers("/public/**").permitAll()

                                                // Static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers("/favicon.ico", "/favicon.svg", "/error").permitAll()

                                                // Fragment endpoints for HTMX
                                                .requestMatchers("/fragments/**").permitAll()

                                                // API endpoints
                                                .requestMatchers("/api/extract", "/api/decode").permitAll()
                                                .requestMatchers("/api/public/**").permitAll()

                                                // Public download for shared messages
                                                .requestMatchers("/download/shared/**").permitAll()

                                                // Protected endpoints
                                                .requestMatchers("/dashboard/**", "/profile/**", "/messages/**")
                                                .authenticated()
                                                .requestMatchers("/api/messages/**", "/api/create-message",
                                                                "/api/encode", "/download/**")
                                                .authenticated()

                                                // Admin endpoints
                                                .requestMatchers("/admin/**").hasRole("ADMIN")

                                                // Everything else requires authentication
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/auth/login")
                                                .loginProcessingUrl("/auth/login")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .successHandler(successHandler())
                                                .failureHandler(customFailureHandler)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessUrl("/")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .key("steganomessages-remember-me")
                                                .tokenValiditySeconds(7 * 24 * 60 * 60)); // 7 days

                return http.build();
        }
}