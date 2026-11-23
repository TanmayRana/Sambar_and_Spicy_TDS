package com.sambarspicy.controller;

import com.sambarspicy.dto.AuthResponse;
import com.sambarspicy.dto.LoginRequest;
import com.sambarspicy.dto.MessageResponse;
import com.sambarspicy.dto.UserDto;
import com.sambarspicy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        UserDto userDto = authService.login(loginRequest, request, response);
        return ResponseEntity.ok(new AuthResponse(userDto, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                               HttpServletResponse response) {
        UserDto userDto = authService.refreshToken(request, response);
        return ResponseEntity.ok(new AuthResponse(userDto, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request,
                                                  HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDto userDto = authService.getCurrentUser();
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken csrfToken) {
        Map<String, String> body = new HashMap<>();
        if (csrfToken != null) {
            body.put("token", csrfToken.getToken());
            body.put("headerName", csrfToken.getHeaderName());
            body.put("parameterName", csrfToken.getParameterName());
        }
        return ResponseEntity.ok(body);
    }
}
