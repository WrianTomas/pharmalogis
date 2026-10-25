package com.pharmalogis.system.modules.iam.application.login;

import com.pharmalogis.system.modules.iam.domain.model.User;
import com.pharmalogis.system.modules.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginHandler {

    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    public LoginResult handle(LoginCommand command) {

        String normalizedEmail = User.normalizeEmail(command.email());

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.rawPassword(), user.encodedPassword()))
            throw new InvalidCredentialsException();

        if (!user.isActive())
            throw new InvalidCredentialsException();

        String token = tokenProvider.generate(user.id().toString());

        return new LoginResult(token, "Bearer");
    }
}
