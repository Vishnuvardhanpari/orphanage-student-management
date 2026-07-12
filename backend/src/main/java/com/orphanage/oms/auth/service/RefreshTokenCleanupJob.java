package com.orphanage.oms.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically purges expired and revoked refresh tokens.
 */
@Component
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanupJob(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purge() {
        refreshTokenService.purgeExpired();
    }
}
