package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.InviteCodeUseCase;
import be.ahm282.QuickClock.domain.model.InviteCode;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.InviteCodeResponseDTO;
import be.ahm282.QuickClock.infrastructure.security.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/invites")
public class InviteController {

    private final InviteCodeUseCase inviteCodeUseCase;
    private final SecurityUtil securityUtil;

    public InviteController(InviteCodeUseCase inviteCodeUseCase, SecurityUtil securityUtil) {
        this.inviteCodeUseCase = inviteCodeUseCase;
        this.securityUtil = securityUtil;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCodeResponseDTO createInvite() {
        securityUtil.requireAdmin();

        InviteCode invite = inviteCodeUseCase.createInviteCode();
        return new InviteCodeResponseDTO(invite.getCode(), invite.getExpiresAt());
    }
}
