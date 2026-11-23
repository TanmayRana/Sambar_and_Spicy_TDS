package com.sambarspicy;

import com.sambarspicy.dto.LoginRequest;
import com.sambarspicy.dto.UserDto;
import com.sambarspicy.enums.UserRole;
import com.sambarspicy.service.AuthService;
import com.sambarspicy.exception.TokenRefreshException;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAdminLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin@123");
        UserDto dto = authService.login(req, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertNotNull(dto);
        assertEquals("admin", dto.getUsername());
        assertEquals(UserRole.ADMIN, dto.getRole());
    }

    @Test
    void testCashierLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setUsername("cashier");
        req.setPassword("Cashier@123");
        UserDto dto = authService.login(req, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertNotNull(dto);
        assertEquals("cashier", dto.getUsername());
        assertEquals(UserRole.BILLING, dto.getRole());
    }

    @Test
    void testViewerLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setUsername("viewer");
        req.setPassword("Viewer@123");
        UserDto dto = authService.login(req, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertNotNull(dto);
        assertEquals("viewer", dto.getUsername());
        assertEquals(UserRole.READ_ONLY, dto.getRole());
    }

    @Test
    void testInvalidPasswordFails() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("WrongPassword@123");
        assertThrows(BadCredentialsException.class, () ->
                authService.login(req, new MockHttpServletRequest(), new MockHttpServletResponse()));
    }

    @Test
    void refreshCookieIsScopedToBothApiVersionsAndRotated() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin@123");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        authService.login(req, new MockHttpServletRequest(), loginResponse);
        String cookie = loginResponse.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("refresh_token="))
                .findFirst().orElseThrow();
        assertTrue(cookie.contains("Path=/api;"));
        assertTrue(cookie.contains("HttpOnly"));
        String rawToken = cookie.substring("refresh_token=".length(), cookie.indexOf(';'));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", rawToken));
        MockHttpServletResponse refreshResponse = new MockHttpServletResponse();
        assertEquals("admin", authService.refreshToken(request, refreshResponse).getUsername());
        assertTrue(refreshResponse.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .anyMatch(value -> value.startsWith("refresh_token=") && value.contains("Path=/api;")));
        assertThrows(TokenRefreshException.class,
                () -> authService.refreshToken(request, new MockHttpServletResponse()));
    }

    @Test
    void sessionMutationsRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf())).andExpect(status().isUnauthorized());
    }
}
