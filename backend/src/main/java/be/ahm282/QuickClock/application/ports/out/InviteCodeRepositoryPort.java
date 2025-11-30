package be.ahm282.QuickClock.application.ports.out;

import be.ahm282.QuickClock.domain.model.InviteCode;

import java.util.List;
import java.util.Optional;

public interface InviteCodeRepositoryPort {
    Optional<InviteCode> findByCode(String code);
    InviteCode save(InviteCode inviteCode);
    List<InviteCode> findAllActive();
}
