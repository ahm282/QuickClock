package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;

public class UserMapper {

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new User(entity.getId(), entity.getUsername(), entity.getSecret());
    }

    public static UserEntity toEntity(User domain, String passwordHash) {
        if (domain == null) {
            return null;
        }

        UserEntity entity = new UserEntity(domain.getUsername(), passwordHash, domain.getSecret());
        entity.setId(domain.getId());
        return entity;
    }
}
