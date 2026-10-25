package com.pharmalogis.system.modules.iam.infraestructure.security;

import com.pharmalogis.system.modules.iam.infraestructure.persistence.JpaUserRepository;
import com.pharmalogis.system.modules.iam.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JpaUserRepository repository;

    public UserDetails loadUserByUsername(String email) {
        var u = repository.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
        return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                .password(u.getEncodedPassword()).roles(u.getRole().name()).disabled(u.getStatus() != UserStatus.ACTIVE)
                .build();
    }
}
