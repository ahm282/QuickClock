package be.ahm282.QuickClock.infrastructure.sse;

import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.QrScanStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QrScanPushService {
    private static final Logger log = LoggerFactory.getLogger(QrScanPushService.class);

    // 30s + 5s skew
    private static final long SSE_TIMEMOUT_MILLIS = Duration.ofSeconds(35).toMillis();

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tokenId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEMOUT_MILLIS);
        emitters.put(tokenId, emitter);

        emitter.onCompletion(() -> emitters.remove(tokenId));
        emitter.onTimeout(() -> {
            emitters.remove(tokenId);
            emitter.complete();
        });
        emitter.onError(ex -> {
            emitters.remove(tokenId);
            log.debug("SSE error for tokenId {}: {}", tokenId, ex.toString());
        });

        // Initial event so the client can start polling immediately
        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("connected"));
        } catch (Exception ex) {
            emitters.remove(tokenId, emitter);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    public void notifyScanned(String tokenId, QrScanStatusDTO status) {
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
            emitter.completeWithError(ex);
        }
    }
}
