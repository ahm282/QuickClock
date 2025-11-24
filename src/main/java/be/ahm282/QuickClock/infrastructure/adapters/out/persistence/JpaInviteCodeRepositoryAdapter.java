package be.ahm282.QuickClock.infrastructure.adapters.out.persistence;

import be.ahm282.QuickClock.application.ports.out.InviteCodeRepositoryPort;
import be.ahm282.QuickClock.domain.model.InviteCode;
import be.ahm282.QuickClock.infrastructure.adapters.out.persistence.mapper.InviteCodeMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaInviteCodeRepositoryAdapter implements InviteCodeRepositoryPort {
    private final JpaInviteCodeRepository jpaInviteCodeRepository;
    private final InviteCodeMapper inviteCodeMapper;

    public JpaInviteCodeRepositoryAdapter(JpaInviteCodeRepository jpaInviteCodeRepository, InviteCodeMapper inviteCodeMapper) {
        this.jpaInviteCodeRepository = jpaInviteCodeRepository;
        this.inviteCodeMapper = inviteCodeMapper;
    }

    @Override
    public Optional<InviteCode> findByCode(String code) {
        return jpaInviteCodeRepository.findByCode(code).map(inviteCodeMapper::toDomain);
    }

    @Override
    public InviteCode save(InviteCode inviteCode) {
        var saved = jpaInviteCodeRepository.save(inviteCodeMapper.toEntity(inviteCode));
        return inviteCodeMapper.toDomain(saved);
    }
}
