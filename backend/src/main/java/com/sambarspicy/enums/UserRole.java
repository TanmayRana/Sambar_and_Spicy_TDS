package com.sambarspicy.enums;

/**
 * Three fixed system user roles.
 * Exactly one user slot per role is permitted by database constraints.
 */
public enum UserRole {
    ADMIN,
    BILLING,
    READ_ONLY
}
