package com.pharmalogis.system.modules.iam.infraestructure.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || header.isBlank() || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String token = header.substring("Bearer ".length());

            if (!jwtService.isValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String subject = jwtService.extractSubject(token);
            GrantedAuthority authority = new SimpleGrantedAuthority(jwtService.extractRole(token));

            Authentication authentication = new UsernamePasswordAuthenticationToken(subject, null, List.of(authority));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            // El token inválido deja la petición sin autenticar.
        }

        filterChain.doFilter(request, response);
    }
}
