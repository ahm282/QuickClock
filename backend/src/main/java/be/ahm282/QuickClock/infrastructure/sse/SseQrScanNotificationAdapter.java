package be.ahm282.QuickClock.infrastructure.sse;

import be.ahm282.QuickClock.application.ports.out.QrScanNotificationPort;
import be.ahm282.QuickClock.infrastructure.adapters.in.web.dto.QrScanStatusDTO;
import org.springframework.stereotype.Component;

@Component
public class SseQrScanNotificationAdapter implements QrScanNotificationPort {
    private final QrScanPushService qrScanPushService;

    public SseQrScanNotificationAdapter(QrScanPushService qrScanPushService) {
        this.qrScanPushService = qrScanPushService;
    }

    @Override
    public void notifyScanned(String tokenId, Long userId, String direction, java.time.Instant clockedAt) {
        qrScanPushService.notifyScanned(tokenId, new QrScanStatusDTO(tokenId, userId, direction, clockedAt));
    }
}
