package com.pharmalogis.system.modules.iam.infraestructure.config;

import com.pharmalogis.system.modules.iam.infraestructure.persistence.*;
import com.pharmalogis.system.modules.iam.domain.model.*;
import com.pharmalogis.system.modules.iam.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class InitialAdmin {
    private final JpaUserRepository users;
    private final PasswordEncoder encoder;

    @Bean
    ApplicationRunner createInitialAdmin(Environment env) {
        return args -> {
            boolean demo = env.matchesProfiles("demo");
            String email = env.getProperty("BOOTSTRAP_ADMIN_EMAIL", "admin@pharmalogis.pe").strip()
                    .toLowerCase(java.util.Locale.ROOT);
            String password = env.getProperty("BOOTSTRAP_ADMIN_PASSWORD", demo ? "DemoPharma2026!" : "");
            if (!password.isBlank() && users.findByEmail(email).isEmpty()) {
                if (password.length() < 12)
                    throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 12 caracteres.");
                users.save(UserEntity.builder().id(UUID.randomUUID()).email(email)
                        .encodedPassword(encoder.encode(password)).role(Role.ADMIN).status(UserStatus.ACTIVE).build());
            }
        };
    }
}
