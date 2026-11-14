package com.rachana.tapewatch.store;

import com.rachana.tapewatch.domain.MarketEvent;
import java.time.Instant;
import java.util.List;

public interface RollingWindowStore {
    void add(String owner, String instrument, MarketEvent event);
    List<MarketEvent> events(String owner, String instrument, Instant since);
    boolean claimAlert(String rule, String owner, String instrument, Instant eventTime, long cooldownSeconds);
}
