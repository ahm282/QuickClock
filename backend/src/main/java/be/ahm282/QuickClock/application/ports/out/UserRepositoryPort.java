package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    List<User> findAllActiveEmployees();
    User save(User user);
}
