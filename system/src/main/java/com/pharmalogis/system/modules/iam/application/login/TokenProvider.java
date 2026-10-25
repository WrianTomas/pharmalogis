package com.pharmalogis.system.modules.iam.application.login;

public interface TokenProvider {

    String generate(String subject);
}
