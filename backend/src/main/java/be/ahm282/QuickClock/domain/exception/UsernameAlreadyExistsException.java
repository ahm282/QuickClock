package be.ahm282.QuickClock.domain.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends DomainException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
