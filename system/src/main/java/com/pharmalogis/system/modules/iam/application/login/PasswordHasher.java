package com.pharmalogis.system.modules.iam.application.login;

public interface PasswordHasher {
    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
