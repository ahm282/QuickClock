package be.ahm282.QuickClock.domain.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
