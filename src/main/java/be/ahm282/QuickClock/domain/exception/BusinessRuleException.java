package be.ahm282.QuickClock.domain.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
