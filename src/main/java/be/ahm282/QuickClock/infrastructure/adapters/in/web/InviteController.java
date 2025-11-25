package be.ahm282.QuickClock.infrastructure.adapters.in.web;

import be.ahm282.QuickClock.application.ports.in.InviteCodeUseCase;
import be.ahm282.QuickClock.domain.model.InviteCode;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.InviteCodeResponseDTO;
import be.ahm282.QuickClock.infrastructure.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/invites")
public class InviteController {

    private final InviteCodeUseCase inviteCodeUseCase;
    private final SecurityUtil securityUtil;

    public InviteController(InviteCodeUseCase inviteCodeUseCase, SecurityUtil securityUtil) {
        this.inviteCodeUseCase = inviteCodeUseCase;
        this.securityUtil = securityUtil;
    }

    // -------------------------------------------------------------------------
    // Create invite
    // -------------------------------------------------------------------------

    @PostMapping // Maps to POST /api/admin/invites
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCodeResponseDTO createInvite(HttpServletRequest request) {
        securityUtil.requireAdmin();
        var userId = securityUtil.extractUserIdFromRequestToken(request);

        InviteCode invite = inviteCodeUseCase.createInviteCode(userId);
        return toDto(invite);
    }

    // -------------------------------------------------------------------------
    // List active invites
    // -------------------------------------------------------------------------

    @GetMapping("/active")
    public List<InviteCodeResponseDTO> listActiveInvites() {
        securityUtil.requireAdmin(); // Use SecurityUtil for auth check

        List<InviteCode> invites = inviteCodeUseCase.listActiveInvites();

        return invites.stream()
                .map(this::toDto)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Revoke invite
    // -------------------------------------------------------------------------

    @PostMapping("/{code}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(@PathVariable String code, HttpServletRequest request) {
        securityUtil.requireAdmin(); // Use SecurityUtil for auth check

        Long adminId = securityUtil.extractUserIdFromRequestToken(request);
        inviteCodeUseCase.revokeInviteCode(code, adminId);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private InviteCodeResponseDTO toDto(InviteCode invite) {
        return new InviteCodeResponseDTO(
                invite.getCode(),
                invite.getExpiresAt(),
                invite.getCreatedByUserId(),
                invite.isUsed(),
                invite.isRevoked(),
                invite.getCreatedAt()
        );
    }
}
