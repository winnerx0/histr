package com.histr.api.utils;

import com.histr.api.model.RefreshToken;
import com.histr.api.model.User;
import com.histr.api.repository.RefreshTokenRepository;
import com.histr.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtUtils jwtUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        try {

            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

            User user = userRepository.findByEmail(oauthUser.getAttribute("email")).orElseThrow(() -> new EntityNotFoundException("User not found"));

            String accessToken = jwtUtils.generateToken(user, "access");

            String refreshToken = jwtUtils.generateToken(user, "refresh");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            RefreshToken token = new RefreshToken();
            token.setTokenHash(HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8))));
            token.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
            token.setUser(user);
            refreshTokenRepository.save(token);

            String url = UriComponentsBuilder.fromUri(URI.create(frontendUrl + "/callback"))
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .toUriString();
            response.sendRedirect(url);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
