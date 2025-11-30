package be.ahm282.QuickClock.domain.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends DomainException {
    public RateLimitException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
