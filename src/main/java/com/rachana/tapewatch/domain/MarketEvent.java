package com.rachana.tapewatch.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketEvent(String eventId, String type, String orderId, String accountId, String instrument,
                          String side, BigDecimal qty, BigDecimal price, String tradeId,
                          String buyAccountId, String sellAccountId, String buyOrderId,
                          String sellOrderId, Instant ts) {
    public String accountFor(String wantedSide) { return "BUY".equals(wantedSide) ? buyAccountId : sellAccountId; }
    public String orderFor(String wantedSide) { return "BUY".equals(wantedSide) ? buyOrderId : sellOrderId; }
}
