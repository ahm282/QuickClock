package be.ahm282.QuickClock.application.ports.in;

import be.ahm282.QuickClock.domain.model.InviteCode;
import java.util.List;

public interface InviteCodeUseCase {
    InviteCode createInviteCode(Long userId);
    List<InviteCode> listActiveInvites();
    void revokeInviteCode(String code, Long revokedByUserId);
}
