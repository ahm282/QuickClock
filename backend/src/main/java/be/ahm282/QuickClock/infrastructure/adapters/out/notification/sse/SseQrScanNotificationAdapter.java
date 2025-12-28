package be.ahm282.QuickClock.infrastructure.sse;

import be.ahm282.QuickClock.application.ports.out.QrScanNotificationPort;
import be.ahm282.QuickClock.application.dto.response.QrScanStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class SseQrScanNotificationAdapter implements QrScanNotificationPort {
    private final QrScanPushService qrScanPushService;

    public SseQrScanNotificationAdapter(QrScanPushService qrScanPushService) {
        this.qrScanPushService = qrScanPushService;
    }

    @Override
    public void notifyScanned(String tokenId, Long userId, String direction, java.time.Instant clockedAt) {
        qrScanPushService.notifyScanned(tokenId, new QrScanStatusResponse(tokenId, userId, direction, clockedAt));
    }
}
