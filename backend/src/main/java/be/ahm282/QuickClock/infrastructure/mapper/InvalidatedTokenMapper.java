package be.ahm282.QuickClock.infrastructure.mapper;

import be.ahm282.QuickClock.domain.model.InvalidatedToken;
import be.ahm282.QuickClock.infrastructure.entity.InvalidatedTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class InvalidatedTokenMapper {
    public InvalidatedToken toDomain(InvalidatedTokenEntity invalidatedTokenEntity) {
        if (invalidatedTokenEntity == null) {
            return null;
        }

        return new InvalidatedToken(invalidatedTokenEntity.getJti(),
                invalidatedTokenEntity.getUserId(),
                invalidatedTokenEntity.getExpiryTime()
        );
    }

    public InvalidatedTokenEntity toEntity(InvalidatedToken invalidatedToken) {
        if (invalidatedToken == null) {
            return null;
        }

        return new InvalidatedTokenEntity(invalidatedToken.getJti(),
                invalidatedToken.getUserId(),
                invalidatedToken.getExpiryTime()
        );
    }
}
