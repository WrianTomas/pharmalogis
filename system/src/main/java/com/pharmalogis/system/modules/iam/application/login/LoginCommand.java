package com.pharmalogis.system.modules.iam.application.login;

public record LoginCommand(String email, String rawPassword) {
}
