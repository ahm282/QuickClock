package be.ahm282.QuickClock.application.ports.out;

import java.time.Instant;

public interface QrScanNotificationPort {
    void notifyScanned(String tokenId, Long userId, String direction, Instant clockedAt);
}
