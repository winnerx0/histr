package com.histr.api.service;

import com.histr.api.dto.AuthResponse;
import com.histr.api.dto.LoginUserRequest;
import com.histr.api.dto.RefreshTokenRequest;
import com.histr.api.dto.RegisterUserRequest;
import com.histr.api.enums.Role;
import com.histr.api.model.RefreshToken;
import com.histr.api.model.User;
import com.histr.api.repository.RefreshTokenRepository;
import com.histr.api.repository.UserRepository;
import com.histr.api.utils.JwtUtils;
import com.histr.api.utils.JwtUtils.TokenType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtils jwtUtils;

    private final AuthenticationProvider authenticationProvider;

    @Transactional
    public AuthResponse register(RegisterUserRequest registerUserRequest) {

        try {

            log.info("register dto {}", registerUserRequest);
            User user = new User();

            user.setUsername(registerUserRequest.getUsername());
            user.setRole(Role.USER);
            user.setEmail(registerUserRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerUserRequest.getPassword()));
            user.setJoinedAt(Instant.now());

            String accessToken = jwtUtils.generateToken(user, "access");

            String refreshToken = jwtUtils.generateToken(user, "refresh");

            userRepository.save(user);

            persistRefreshToken(user, refreshToken);

            return new AuthResponse(accessToken, refreshToken);
        } catch (NoSuchAlgorithmException  e) {
            throw new RuntimeException(e);
        }
    }

    public AuthResponse login(LoginUserRequest loginUserRequest) {

        try {

            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(loginUserRequest.getUsername(), loginUserRequest.getPassword()));

            User user = userRepository.findByUsername(loginUserRequest.getUsername()).orElseThrow(() -> new EntityNotFoundException("User not found"));

            String accessToken = jwtUtils.generateToken(user, "access");

            String refreshToken = jwtUtils.generateToken(user, "refresh");

            persistRefreshToken(user, refreshToken);

            return new AuthResponse(accessToken, refreshToken);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        try {
            String presented = request.getRefreshToken();
            String hash = hashRefreshToken(presented);

            RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                    .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

            if (stored.isBlacklisted()) {
                // Reuse of a rotated token — likely theft. Burn every refresh token for this user.
                refreshTokenRepository.blacklistAllForUser(stored.getUser());
                throw new BadCredentialsException("Refresh token has been revoked");
            }

            if (stored.getExpiresAt().isBefore(Instant.now())) {
                throw new BadCredentialsException("Refresh token has expired");
            }

            User user = stored.getUser();

            // Verify signature against the refresh secret + that the subject still matches.
            String subject;
            try {
                subject = jwtUtils.extractUsername(presented, TokenType.REFRESH);
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            if (!user.getUsername().equals(subject)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            // Rotate: blacklist the presented one, issue a new pair.
            stored.setBlacklisted(true);
            refreshTokenRepository.save(stored);

            String newAccess = jwtUtils.generateToken(user, "access");
            String newRefresh = jwtUtils.generateToken(user, "refresh");
            persistRefreshToken(user, newRefresh);

            return new AuthResponse(newAccess, newRefresh);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthResponse oauthSuccess(OidcUser oidcUser){
        User user = userRepository.findByEmail(oidcUser.getEmail()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        String accessToken = jwtUtils.generateToken(user, "access");

        String refreshToken = jwtUtils.generateToken(user, "refresh");

        try {
            persistRefreshToken(user, refreshToken);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return new AuthResponse(accessToken, refreshToken);
    }

    private void persistRefreshToken(User user, String refreshToken) throws NoSuchAlgorithmException {
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hashRefreshToken(refreshToken));
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        token.setUser(user);
        refreshTokenRepository.save(token);
    }

    private String hashRefreshToken(String refreshToken) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
    }
}
