package com.orphanage.oms.auth.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class RefreshTokenCleanupJobTest {

    @Test
    void purgeDelegatesToRefreshTokenService() {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCleanupJob job = new RefreshTokenCleanupJob(refreshTokenService);

        job.purge();

        verify(refreshTokenService).purgeExpired();
    }
}
