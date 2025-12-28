package be.ahm282.QuickClock.domain.exception;

public class UsernameAlreadyExistsException extends DomainException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
