package com.am.marketdata.service.ipo;

import com.am.marketdata.common.ipo.IpoIssue;
import com.am.marketdata.common.ipo.IpoIssueSource;
import com.am.marketdata.common.ipo.IpoIssueStatus;
import com.am.marketdata.common.ipo.IpoLifecycle;
import com.am.marketdata.service.model.IpoIssueDocument;
import com.am.marketdata.service.repo.IpoIssueRepository;
import com.am.marketdata.service.repo.IpoSyncMetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpoSyncServiceTest {

    @Mock
    private IpoIssueSource ipoIssueSource;
    @Mock
    private IpoIssueRepository ipoIssueRepository;
    @Mock
    private IpoSyncMetaRepository ipoSyncMetaRepository;

    private IpoSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new IpoSyncService(
                Optional.of(ipoIssueSource), ipoIssueRepository, ipoSyncMetaRepository);
    }

    @Test
    void mergeLifecycleIsMonotonicTowardPast() {
        assertEquals("CURRENT", IpoSyncService.mergeLifecycle("UPCOMING", "CURRENT"));
        assertEquals("PAST", IpoSyncService.mergeLifecycle("CURRENT", "PAST"));
        assertEquals("CURRENT", IpoSyncService.mergeLifecycle("CURRENT", "UPCOMING"));
        assertEquals("PAST", IpoSyncService.mergeLifecycle("PAST", "CURRENT"));
        assertEquals("UPCOMING", IpoSyncService.mergeLifecycle(null, "UPCOMING"));
    }

    @Test
    void upsertCreatesIdAndDoesNotOverwriteWithNull() {
        LocalDate open = LocalDate.of(2026, 7, 30);
        when(ipoIssueRepository.findById("JNPR:2026-07-30")).thenReturn(Optional.of(
                IpoIssueDocument.builder()
                        .id("JNPR:2026-07-30")
                        .symbol("JNPR")
                        .companyName("Juniper Hotels Limited")
                        .series("EQ")
                        .lifecycle("CURRENT")
                        .status("ACTIVE")
                        .openDate(open)
                        .priceMin(100.0)
                        .priceMax(110.0)
                        .issueSizeLabel("kept-label")
                        .onNse(true)
                        .build()));

        IpoIssue incoming = new IpoIssue(
                "JNPR",
                "juniper-hotels",
                null,
                "EQ",
                IpoLifecycle.PAST,
                IpoIssueStatus.CLOSED,
                open,
                LocalDate.of(2026, 8, 1),
                null,
                null,
                "INR",
                100.0,
                110.0,
                105.0,
                "Rs.100 to Rs.110",
                25_000_000L,
                null,
                true,
                true,
                2.5,
                50_000_000L,
                25_000_000L);

        Instant now = Instant.parse("2026-08-04T10:00:00Z");
        syncService.upsertIssue(incoming, "PAST", "NSE", now);

        ArgumentCaptor<IpoIssueDocument> captor = ArgumentCaptor.forClass(IpoIssueDocument.class);
        verify(ipoIssueRepository).save(captor.capture());
        IpoIssueDocument saved = captor.getValue();
        assertEquals("JNPR:2026-07-30", saved.getId());
        assertEquals("Juniper Hotels Limited", saved.getCompanyName());
        assertEquals("kept-label", saved.getIssueSizeLabel());
        assertEquals("PAST", saved.getLifecycle());
        assertEquals(105.0, saved.getIssuePrice());
        assertEquals(2.5, saved.getSubscriptionTimes());
        assertTrue(saved.isOnBse());
        assertEquals(now, saved.getSyncedAt());
    }
}
