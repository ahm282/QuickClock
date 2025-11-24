package be.ahm282.QuickClock.infrastructure.adapters.out.hibp;

import be.ahm282.QuickClock.application.ports.out.BreachedPasswordCheckPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Component
public class HIBPBreachedPasswordAdapter implements BreachedPasswordCheckPort {
    private static final Logger log = LoggerFactory.getLogger(HIBPBreachedPasswordAdapter.class);
    private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";

    private final HttpClient httpClient;

    public HIBPBreachedPasswordAdapter() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    @Override
    public int getBreachCount(String password) throws IOException, InterruptedException, NoSuchAlgorithmException {
        String sha1 = sha1Hex(password).toUpperCase();
        String prefix = sha1.substring(0, 5);
        String suffix = sha1.substring(5);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HIBP_API_URL + prefix))
                .header("Add-Padding", "true")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("HIBP returned {}: {}", response.statusCode(), response.body());
            throw new IOException("HIBP error: " + response.statusCode());
        }

        for (String line : response.body().split("\r\n")) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }

    private String sha1Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hashBytes.length * 2);
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
