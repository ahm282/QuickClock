package be.ahm282.QuickClock.domain.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends DomainException {
    public AuthenticationException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
