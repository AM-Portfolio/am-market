package com.am.marketdata.common.observability;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Slf4j markers to distinguish flow logs from regular logs.
 */
public final class FlowMarkers {
    public static final Marker FLOW = MarkerFactory.getMarker("FLOW");

    private FlowMarkers() {}
}
