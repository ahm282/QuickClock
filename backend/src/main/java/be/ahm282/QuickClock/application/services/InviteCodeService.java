package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.InviteCodeUseCase;
import be.ahm282.QuickClock.application.ports.out.InviteCodeRepositoryPort;
import be.ahm282.QuickClock.domain.exception.BusinessRuleException;
import be.ahm282.QuickClock.domain.model.InviteCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static java.time.Instant.now;

@Service
public class InviteCodeService implements InviteCodeUseCase {
    private static final Duration INVITE_CODE_TTL = Duration.ofHours(12);

    private final InviteCodeRepositoryPort inviteCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public InviteCodeService(InviteCodeRepositoryPort inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    @Override
    public InviteCode createInviteCode(Long createdByUserId) {
        String code = generateUniqueCode();
        Instant expiresAt = now().plus(INVITE_CODE_TTL);
        InviteCode inviteCode = new InviteCode(
                null,
                code,
                expiresAt,
                false,
                false,
                null,
                null,
                createdByUserId,
                null,
                now()
        );

        return inviteCodeRepository.save(inviteCode);
    }

    @Override
    public List<InviteCode> listActiveInvites() {
        return inviteCodeRepository.findAllActive();
    }

    @Override
    public void revokeInviteCode(String code, Long revokedByUserId) {
        InviteCode inviteCode = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessRuleException("Invite code not found"));

        Instant now = now();

        if (inviteCode.isExpired()) {
            throw new BusinessRuleException("Invite code has expired");
        }

        if (inviteCode.used()) {
            throw new BusinessRuleException("Invite code has already been used");
        }

        if (inviteCode.revoked()) {
            throw new BusinessRuleException("Invite code has already been revoked");
        }

        InviteCode revokedInviteCode = inviteCode.revoke(revokedByUserId, now());
        inviteCodeRepository.save(revokedInviteCode);
    }

    private String generateUniqueCode() {
        byte[] bytes = new byte[16];
        while (true) {
            secureRandom.nextBytes(bytes);
            String code = HexFormat.of().formatHex(bytes);
            if (inviteCodeRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
    }
}
