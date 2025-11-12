package be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper;

import be.ahm282.QuickClock.domain.model.User;
import be.ahm282.QuickClock.infrastructure.entity.UserEntity;

public class UserMapper {

    public static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getSecret());
    }

    public static UserEntity toEntity(User domain) {
        return new UserEntity(domain.getId(), domain.getUsername(), domain.getSecret());
    }
}
