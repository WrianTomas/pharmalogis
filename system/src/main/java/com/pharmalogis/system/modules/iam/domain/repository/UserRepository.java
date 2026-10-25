package com.pharmalogis.system.modules.iam.domain.repository;

import com.pharmalogis.system.modules.iam.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    User save(User user);
}
