package com.sambarspicy.bootstrap;

import com.sambarspicy.enums.UserRole;
import com.sambarspicy.model.User;
import com.sambarspicy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import com.sambarspicy.security.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

/**
 * Ensures the three fixed system accounts exist in the database on startup.
 * Required for local development, fresh schemas, and integration tests.
 * Fixed roles: (1: ADMIN), (2: BILLING), (3: READ_ONLY).
 */
@Component
public class UserBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final JwtProperties jwtProperties;

    @Value("${app.bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.bootstrap.cashier.password:Cashier@123}")
    private String cashierPassword;

    @Value("${app.bootstrap.viewer.password:Viewer@123}")
    private String viewerPassword;

    public UserBootstrapRunner(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               Environment environment,
                               JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateProductionSafety();

        bootstrapUserIfMissing(1, UserRole.ADMIN, "admin", "System Administrator", adminPassword);
        bootstrapUserIfMissing(2, UserRole.BILLING, "cashier", "Counter Cashier", cashierPassword);
        bootstrapUserIfMissing(3, UserRole.READ_ONLY, "viewer", "Auditor Viewer", viewerPassword);
    }

    private void validateProductionSafety() {
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

        if (isProd) {
            if (JwtProperties.DEVELOPMENT_SECRET.equals(jwtProperties.getSecret())
                    || !jwtProperties.isCookieSecure()) {
                throw new IllegalStateException("Production requires a unique JWT secret and secure HTTPS cookies");
            }
            if ("Admin@123".equals(adminPassword) || "Cashier@123".equals(cashierPassword) || "Viewer@123".equals(viewerPassword)) {
                log.error("CRITICAL SECURITY RISK: Default demo passwords detected in production profile! " +
                        "Override app.bootstrap.*.password using secure environment variables.");
                throw new IllegalStateException("Default demo passwords are strictly prohibited in production.");
            }
        }
    }

    private void bootstrapUserIfMissing(Integer id, UserRole role, String username, String fullName, String rawPassword) {
        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (!role.equals(existingUser.getRole())) {
                throw new IllegalStateException("Fixed user slot " + id + " has an unexpected role");
            }
            log.debug("Fixed user slot {} [{}] already exists; preserving account settings.", id, role);
        } else {
            User newUser = new User(id, role, username, fullName, passwordEncoder.encode(rawPassword));
            userRepository.save(newUser);
            log.info("Bootstrapped fixed system user: {} [{}]", username, role);
        }
    }
}
