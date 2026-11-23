package com.sambarspicy.service;

import com.sambarspicy.dto.LoginRequest;
import com.sambarspicy.dto.UserDto;
import com.sambarspicy.model.AuthRefreshToken;
import com.sambarspicy.model.User;
import com.sambarspicy.exception.TokenRefreshException;
import com.sambarspicy.repository.AuthRefreshTokenRepository;
import com.sambarspicy.repository.UserRepository;
import com.sambarspicy.security.CustomUserDetails;
import com.sambarspicy.security.JwtProperties;
import com.sambarspicy.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthRefreshTokenRepository authRefreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       AuthRefreshTokenRepository authRefreshTokenRepository,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.authRefreshTokenRepository = authRefreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserDto login(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new DisabledException("User account is inactive");
        }

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate Access Token (JWT)
        String accessToken = tokenProvider.generateAccessToken(userDetails);

        // Generate and persist Refresh Token (opaque token hashed with SHA-256)
        String rawRefreshToken = tokenProvider.generateRawRefreshToken();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenExpirationDays());

        AuthRefreshToken refreshToken = new AuthRefreshToken(
                user,
                tokenHash,
                refreshExpiry
        );
        authRefreshTokenRepository.save(refreshToken);

        // Set HttpOnly Cookies
        setAuthCookies(response, accessToken, rawRefreshToken);

        return toDto(user);
    }

    @Transactional
    public UserDto refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshToken(request);
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new TokenRefreshException("Refresh token is missing");
        }

        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        AuthRefreshToken refreshToken = authRefreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null) {
            throw new TokenRefreshException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new DisabledException("User account is deactivated");
        }

        // Token rotation: Revoke existing refresh token
        refreshToken.setRevokedAt(LocalDateTime.now());
        authRefreshTokenRepository.save(refreshToken);

        // Issue new refresh token
        String newRawRefreshToken = tokenProvider.generateRawRefreshToken();
        String newTokenHash = tokenProvider.hashToken(newRawRefreshToken);
        LocalDateTime newRefreshExpiry = LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenExpirationDays());

        AuthRefreshToken newRefreshToken = new AuthRefreshToken(
                user,
                newTokenHash,
                newRefreshExpiry
        );
        authRefreshTokenRepository.save(newRefreshToken);

        // Generate new Access Token
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = tokenProvider.generateAccessToken(userDetails);

        // Update Cookies
        setAuthCookies(response, newAccessToken, newRawRefreshToken);

        return toDto(user);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshToken(request);
        if (StringUtils.hasText(rawRefreshToken)) {
            String tokenHash = tokenProvider.hashToken(rawRefreshToken);
            authRefreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
                if (token.getRevokedAt() == null) {
                    token.setRevokedAt(LocalDateTime.now());
                    authRefreshTokenRepository.save(token);
                }
            });
        }

        // Clear HttpOnly Cookies
        clearAuthCookies(response);
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user in context");
        }

        String username;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return toDto(user);
    }

    public void setAuthCookies(HttpServletResponse response, String accessToken, String rawRefreshToken) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.getAccessTokenExpirationMs()))
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawRefreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/api")
                .maxAge(Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays()))
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true).secure(jwtProperties.isCookieSecure()).path("/api/auth")
                .maxAge(0).sameSite(jwtProperties.getCookieSameSite()).build().toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/")
                .maxAge(0)
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .path("/api")
                .maxAge(0)
                .sameSite(jwtProperties.getCookieSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
        // Remove cookies issued by earlier releases at the narrower path too.
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true).secure(jwtProperties.isCookieSecure()).path("/api/auth")
                .maxAge(0).sameSite(jwtProperties.getCookieSameSite()).build().toString());
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        String headerToken = request.getHeader("X-Refresh-Token");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        return null;
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
