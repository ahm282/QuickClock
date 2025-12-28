package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;

        return User.builder()
                .id(entity.getId())
                .publicId(entity.getPublicId())
                .username(entity.getUsername())
                .displayName(entity.getDisplayName())
                .displayNameArabic(entity.getDisplayNameArabic())
                .passwordHash(entity.getPasswordHash())
                .secret(entity.getSecret())
                .roles(entity.getRoles())
                .accountType(entity.getAccountType())
                .active(entity.isActive())
                .lastLogin(entity.getLastLogin())
                .lastPasswordChange(entity.getLastPasswordChange())
                .failedLoginAttempts(entity.getFailedLoginAttempts())
                .lockedUntil(entity.getLockedUntil())
                .build();
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
        entity.setDisplayNameArabic(user.getDisplayNameArabic());

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
