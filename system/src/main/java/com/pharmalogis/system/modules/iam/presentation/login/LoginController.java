package com.pharmalogis.system.modules.iam.presentation.login;

import com.pharmalogis.system.modules.iam.application.login.LoginCommand;
import com.pharmalogis.system.modules.iam.application.login.LoginHandler;
import com.pharmalogis.system.modules.iam.application.login.LoginResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth/login/")
@RequiredArgsConstructor
public class LoginController {

    private final LoginHandler loginHandler;

    @PostMapping("")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginHandler.handle(new LoginCommand(request.email(), request.password()));

        LoginResponse response = new LoginResponse(result.accessToken(), result.tokenType());

        return ResponseEntity.ok(response);
    }

}
