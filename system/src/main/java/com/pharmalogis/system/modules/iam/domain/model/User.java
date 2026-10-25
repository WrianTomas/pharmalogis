package com.pharmalogis.system.modules.iam.domain.model;

import com.pharmalogis.system.modules.iam.domain.exception.InvalidEmailException;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final UUID id;

    private final String email;
    private final String encodedPassword;
    private final Role role;
    private final UserStatus status;

    public User(String email, String encodedPassword, Role role, UserStatus status) {
        this(UUID.randomUUID(), email, encodedPassword, role, status);
    }

    public User(UUID id, String email, String encodedPassword, Role role, UserStatus status) {
        this.id = Objects.requireNonNull(id, "The ID is required");
        this.email = normalizeEmail(email);
        this.encodedPassword = requiredText(encodedPassword, "The encoded password is required");
        this.role = Objects.requireNonNull(role, "The role is required");
        this.status = Objects.requireNonNull(status, "The status is required");
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public String encodedPassword() {
        return encodedPassword;
    }

    public String email() {
        return email;
    }

    public Role role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    private String requiredText(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank())
            throw new IllegalArgumentException(message);
        return value;
    }

    private static boolean isValidEmail(String email) {
        if (email.chars().anyMatch(Character::isWhitespace))
            return false;
        int atIndex = email.indexOf('@');
        return atIndex > 0 && atIndex == email.lastIndexOf('@') && atIndex < email.length() - 1;
    }

    // Conserva las mayúsculas del usuario y normaliza solo el dominio.
    public static String normalizeEmail(String email) {
        Objects.requireNonNull(email, "The email is required");
        if (email.isBlank())
            throw new InvalidEmailException("The email is required");
        if (!email.equals(email.strip()))
            throw new InvalidEmailException("Email cannot contain surrounding whitespace");
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@') || atIndex == email.length() - 1) {
            throw new InvalidEmailException("Email invalid");
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1).toLowerCase(Locale.ROOT);
        if (!isValidEmail(localPart + "@" + domain))
            throw new InvalidEmailException("Email invalid");
        return localPart + "@" + domain;
    }
}
