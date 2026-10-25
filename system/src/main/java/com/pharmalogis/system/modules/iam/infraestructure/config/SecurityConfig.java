package com.pharmalogis.system.modules.iam.infraestructure.config;

import com.pharmalogis.system.modules.iam.infraestructure.security.*;
import com.pharmalogis.system.modules.iam.infraestructure.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http, JwtAuthenticationFilter jwt, RestAuthenticationEntryPoint entry,
            RestAccessDeniedHandler denied) throws Exception {
        return http.securityMatcher("/api/**").csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a -> a.requestMatchers("/api/v1/auth/**").permitAll().anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain web(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(a -> a.requestMatchers("/login", "/css/**", "/error").permitAll().anyRequest()
                        .hasAnyRole("ADMIN", "RRHH"))
                .formLogin(
                        f -> f.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/", true).permitAll())
                .logout(l -> l.logoutSuccessUrl("/login?logout")).build();
    }
}