package be.ahm282.QuickClock.infrastructure.adapters.out.notification.sse;

import be.ahm282.QuickClock.application.dto.response.QrScanStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QrScanPushService {
    private static final Logger log = LoggerFactory.getLogger(QrScanPushService.class);

    // 30s + 5s skew
    private static final long SSE_TIMEOUT_MILLIS = Duration.ofSeconds(35).toMillis();

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tokenId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.put(tokenId, emitter);

        emitter.onCompletion(() -> emitters.remove(tokenId));
        emitter.onTimeout(() -> {
            emitters.remove(tokenId);
            emitter.complete();
        });
        emitter.onError(ex -> {
            emitters.remove(tokenId);
            log.debug("SSE connection error for tokenId {}: {}", tokenId, ex.getMessage());
        });

        // Initial event so the client can start polling immediately
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("init")
                        .data("connected"));
            } catch (Exception ex) {
                emitters.remove(tokenId);
            }
        });

        return emitter;
    }

    public void notifyScanned(String tokenId, QrScanStatusResponse status) {
        SseEmitter emitter = emitters.remove(tokenId);
        if (emitter == null) {
            log.debug("No active SSE emitter for tokenId {}, dropping scan notification", tokenId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("scanned")
                    .data(status));
            emitter.complete();
        } catch (IOException ex) {
            log.debug("Failed to push scanned event for tokenId {}: {}", tokenId, ex.toString());
        }
    }

    /**
     * Sends a heartbeat every 15 seconds to keep connections alive
     * and prevent proxy timeouts.
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        log.trace("Sending heartbeat to {} active emitters", emitters.size());

        emitters.forEach((tokenId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("keep-alive"));
            } catch (IOException e) {
                // If heartbeat fails, the connection is likely dead.
                emitters.remove(tokenId);
                log.debug("Heartbeat failed for {}, removing emitter.", tokenId);
            }
        });
    }
}
