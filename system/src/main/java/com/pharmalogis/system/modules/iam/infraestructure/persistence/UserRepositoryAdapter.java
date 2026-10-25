package com.pharmalogis.system.modules.iam.infraestructure.persistence;

import com.pharmalogis.system.modules.iam.domain.model.User;
import com.pharmalogis.system.modules.iam.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = toEntity(user);

        UserEntity saved = jpaUserRepository.save(userEntity);

        return toDomain(saved);
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getEncodedPassword(), entity.getRole(),
                entity.getStatus());
    }

    private UserEntity toEntity(User domain) {
        return UserEntity.builder().id(domain.id()).email(domain.email()).encodedPassword(domain.encodedPassword())
                .role(domain.role()).status(domain.status()).build();
    }
}
