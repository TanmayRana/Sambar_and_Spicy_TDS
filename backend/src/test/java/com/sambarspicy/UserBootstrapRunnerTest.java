package com.sambarspicy;

import com.sambarspicy.bootstrap.UserBootstrapRunner;
import com.sambarspicy.enums.UserRole;
import com.sambarspicy.model.User;
import com.sambarspicy.repository.UserRepository;
import com.sambarspicy.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBootstrapRunnerTest {
    @Test
    void existingAccountsKeepTheirPasswordUsernameAndInactiveState() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        User admin = new User(1, UserRole.ADMIN, "renamed-admin", "Admin", "changed-hash");
        User cashier = new User(2, UserRole.BILLING, "cashier", "Cashier", "cashier-hash");
        cashier.setIsActive(false);
        User viewer = new User(3, UserRole.READ_ONLY, "viewer", "Viewer", "viewer-hash");
        when(users.findById(1)).thenReturn(Optional.of(admin));
        when(users.findById(2)).thenReturn(Optional.of(cashier));
        when(users.findById(3)).thenReturn(Optional.of(viewer));

        new UserBootstrapRunner(users, encoder, environment, new JwtProperties()).run(null);

        assertEquals("renamed-admin", admin.getUsername());
        assertEquals("changed-hash", admin.getPasswordHash());
        assertFalse(cashier.getIsActive());
        verify(users, never()).save(any(User.class));
        verify(encoder, never()).encode(any());
    }

    @Test
    void productionRejectsDevelopmentJwtSecretAndInsecureCookies() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        UserBootstrapRunner runner = new UserBootstrapRunner(
                mock(UserRepository.class), mock(PasswordEncoder.class), environment, new JwtProperties());
        assertThrows(IllegalStateException.class, () -> runner.run(null));
    }
}
