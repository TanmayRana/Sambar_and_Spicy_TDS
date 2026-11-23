package com.sambarspicy.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    public static final String DEVELOPMENT_SECRET =
            "SambarAndSpicyRestaurantSecretKeyForJwt2026SecureMustBe256BitsLong!";

    /**
     * Secret key for HMAC-SHA256 signing (at least 256 bits / 32 bytes).
     */
    private String secret = DEVELOPMENT_SECRET;

    /**
     * Access token lifetime in milliseconds (default: 15 minutes = 900,000 ms).
     */
    private long accessTokenExpirationMs = 900_000L;

    /**
     * Refresh token lifetime in days (default: 7 days).
     */
    private int refreshTokenExpirationDays = 7;

    /**
     * Whether cookies should have the Secure flag (true in production HTTPS).
     */
    private boolean cookieSecure = false;

    /**
     * SameSite attribute for auth cookies ("Lax" or "Strict").
     */
    private String cookieSameSite = "Lax";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public int getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    public void setRefreshTokenExpirationDays(int refreshTokenExpirationDays) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }
}
