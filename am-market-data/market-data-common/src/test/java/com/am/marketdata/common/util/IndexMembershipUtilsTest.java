package com.am.marketdata.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexMembershipUtilsTest {

    @Test
    void testStandardSizeIndices() {
        assertEquals(490, IndexMembershipUtils.minimumMembershipSize("NIFTY 500"));
        assertEquals(190, IndexMembershipUtils.minimumMembershipSize("NIFTY 200"));
        assertEquals(95, IndexMembershipUtils.minimumMembershipSize("NIFTY 100"));
        assertEquals(45, IndexMembershipUtils.minimumMembershipSize("NIFTY 50"));
        assertEquals(45, IndexMembershipUtils.minimumMembershipSize("NIFTY NEXT 50"));
    }

    @Test
    void testIntermediateIndicesNotMatchingFifty() {
        // Must not match "50" false-positive
        assertEquals(140, IndexMembershipUtils.minimumMembershipSize("NIFTY MIDCAP 150"));
        assertEquals(240, IndexMembershipUtils.minimumMembershipSize("NIFTY SMLCAP 250"));
    }

    @Test
    void testSectoralIndices() {
        assertEquals(8, IndexMembershipUtils.minimumMembershipSize("NIFTY BANK"));
        assertEquals(8, IndexMembershipUtils.minimumMembershipSize("NIFTY IT"));
        assertEquals(12, IndexMembershipUtils.minimumMembershipSize("NIFTY AUTO"));
        assertEquals(15, IndexMembershipUtils.minimumMembershipSize("NIFTY PHARMA"));
    }

    @Test
    void testEdgeCases() {
        assertEquals(1, IndexMembershipUtils.minimumMembershipSize(null));
        assertEquals(1, IndexMembershipUtils.minimumMembershipSize(""));
        assertEquals(1, IndexMembershipUtils.minimumMembershipSize("CUSTOM_UNKNOWN_INDEX"));
    }
}
