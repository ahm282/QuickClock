package be.ahm282.QuickClock.domain.exception;

public class TokenException extends DomainException {
    private Long userId;

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
