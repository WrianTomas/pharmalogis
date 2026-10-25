package com.pharmalogis.system.modules.iam.infraestructure.security.jwt;

import com.pharmalogis.system.modules.iam.application.login.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService implements TokenProvider {

    private final SecretKey secret;
    private final long expiration;
    private final com.pharmalogis.system.modules.iam.infraestructure.persistence.JpaUserRepository users;

    public JwtService(SecretKey secret, JwtProperties jwtProperties,
            com.pharmalogis.system.modules.iam.infraestructure.persistence.JpaUserRepository users) {
        this.secret = secret;
        this.expiration = jwtProperties.expiration();
        this.users = users;
    }

    @Override
    public String generate(String subject) {
        return Jwts.builder().subject(subject)
                .claim("role",
                        "ROLE_" + users.findById(java.util.UUID.fromString(subject)).orElseThrow().getRole().name())
                .issuedAt(Date.from(Instant.now())).expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secret).compact();
    }

    public boolean isValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(secret).build().parseSignedClaims(token).getPayload();
    }
}
