package be.ahm282.QuickClock.infrastructure.mapper;

import be.ahm282.QuickClock.domain.model.RefreshToken;
import be.ahm282.QuickClock.infrastructure.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for RefreshToken domain model and entity.
 */
@Component
public class RefreshTokenMapper {

    public RefreshTokenEntity toEntity(RefreshToken domain) {
        if (domain == null) {
            return null;
        }
        return new RefreshTokenEntity(
                domain.getJti(),
                domain.getParentId(),
                domain.getRootFamilyId(),
                domain.getUserId(),
                domain.isRevoked(),
                domain.isUsed(),
                domain.getIssuedAt(),
                domain.getExpiresAt()
        );
    }

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RefreshToken(
                entity.getJti(),
                entity.getParentId(),
                entity.getRootFamilyId(),
                entity.getUserId(),
                entity.isRevoked(),
                entity.isUsed(),
                entity.getIssuedAt(),
                entity.getExpiresAt()
        );
    }
}

