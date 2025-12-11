package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.application.ports.out.UserRepositoryPort;
import be.ahm282.QuickClock.domain.exception.UsernameAlreadyExistsException;
import be.ahm282.QuickClock.domain.model.AccountType;
import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.UserMapper;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<User> findByPublicId(UUID publicId) {
        return repository.findByPublicId(publicId).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllActiveEmployees() {
        return repository.findAllByActiveTrueAndAccountType(AccountType.EMPLOYEE)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = mapper.toEntity(user);

        try {
            UserEntity saved = repository.save(userEntity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isUsernameUniqueConstraintViolation(ex)) {
                throw new UsernameAlreadyExistsException("Username already exists");
            }
            throw ex; // rethrow other integrity violations
        }
    }

    private boolean isUsernameUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable root = ex.getMostSpecificCause();
        String msg = root.getMessage();

        if (msg == null) return false;
        return msg.toLowerCase().contains("uk_users_username");
    }
}