package be.ahm282.QuickClock.application.ports.out;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface BreachedPasswordCheckPort {
    /**
     * @return number of times this password has appeared in known breaches.
     */
    int getBreachCount(String password) throws IOException, InterruptedException, NoSuchAlgorithmException;
}
