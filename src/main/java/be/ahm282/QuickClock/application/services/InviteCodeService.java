package be.ahm282.QuickClock.application.services;

import be.ahm282.QuickClock.application.ports.in.InviteCodeUseCase;
import be.ahm282.QuickClock.application.ports.out.InviteCodeRepositoryPort;
import be.ahm282.QuickClock.domain.model.InviteCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class InviteCodeService implements InviteCodeUseCase {
    private static final Duration INVITE_CODE_TTL = Duration.ofHours(12);

    private final InviteCodeRepositoryPort inviteCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public InviteCodeService(InviteCodeRepositoryPort inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    @Override
    public InviteCode createInviteCode() {
        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plus(INVITE_CODE_TTL);

        InviteCode inviteCode = new InviteCode(null, code, expiresAt, false, null);
        return inviteCodeRepository.save(inviteCode);
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
