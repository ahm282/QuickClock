package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new User(
                entity.getId(),
                entity.getPublicId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                entity.getSecret(),
                entity.getRoles(),
                entity.getAccountType(),
                entity.isActive(),
                entity.getLastLogin(),
                entity.getLastPasswordChange(),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil()
        );
    }

    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();

        entity.setId(user.getId());
        entity.setPublicId(user.getPublicId());

        entity.setUsername(user.getUsername());
        entity.setDisplayName(user.getDisplayName());

        entity.setPasswordHash(user.getPasswordHash());
        entity.setSecret(user.getSecret());

        entity.setRoles(user.getRoles());
        entity.setAccountType(user.getAccountType());

        entity.setActive(user.isActive());

        entity.setLastLogin(user.getLastLogin());
        entity.setLastPasswordChange(user.getLastPasswordChange());

        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockedUntil(user.getLockedUntil());

        return entity;
    }
}
