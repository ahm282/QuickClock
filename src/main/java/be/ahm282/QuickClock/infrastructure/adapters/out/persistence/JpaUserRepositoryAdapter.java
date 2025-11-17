package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.UserMapper;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserRepositoryAdapter implements UserRepositoryPort {
    private final JpaUserRepository repository;
    private final UserMapper mapper;

    public JpaUserRepositoryAdapter(JpaUserRepository userRepository, UserMapper mapper) {
        repository = userRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity saved = repository.save(mapper.toEntity(user));
        return mapper.toDomain(saved);
    }
}