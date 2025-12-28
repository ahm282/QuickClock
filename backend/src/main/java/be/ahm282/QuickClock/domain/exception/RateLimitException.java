package be.ahm282.QuickClock.domain.exception;

public class RateLimitException extends DomainException {
    public RateLimitException(String message) {
        super(message);
    }
}