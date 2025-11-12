package be.ahm282.QuickClock.domain.model;

import be.ahm282.QuickClock.domain.exception.ValidationException;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Domain model for User.
 * This class contains business logic and validation, but no persistence
 * annotations.
 */
public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;

    private final Set<ClockRecord> timeEntries = new HashSet<>();

    // Private constructor for persistence mapping
    private User() {}

    public User(Long id, String username, String email, String password, String firstName, String lastName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.validate();
    }

    public static User createUser(String username, String email, String password, String firstName, String lastName) {
        // ID is null for creation
        return new User(null, username, email, password, firstName, lastName);
    }

    /**
     * Enforces the business rules (invariants) for a User.
     */
    public void validate() {
        if (!StringUtils.hasText(username) || username.length() > 50) {
            throw new ValidationException("Username must be between 1 and 50 characters.");
        }
        if (!StringUtils.hasText(email) || !email.contains("@") || email.length() > 100) {
            throw new ValidationException("Invalid email format or length.");
        }
        if (!StringUtils.hasText(password)) {
            // In a real app, you'd check password hash, not plaintext
            throw new ValidationException("Password cannot be blank.");
        }
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            throw new ValidationException("First and last name are required.");
        }
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    // --- Business Logic ---

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
