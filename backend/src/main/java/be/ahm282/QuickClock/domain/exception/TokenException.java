package be.ahm282.QuickClock.domain.exception;

public class TokenException extends DomainException {
    private final Long userId;

    public TokenException(String message) {
        super(message);
        this.userId = null;
    }

    public TokenException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}