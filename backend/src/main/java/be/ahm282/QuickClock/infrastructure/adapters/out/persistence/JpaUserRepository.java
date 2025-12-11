package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.domain.model.AccountType;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    @Override
    Optional<UserEntity> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByPublicId(UUID publicId);

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.active = true AND u.accountType = :accountType")
    List<UserEntity> findAllByActiveTrueAndAccountType(@Param("accountType") AccountType accountType);
}