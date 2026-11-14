package com.rachana.tapewatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "surveillance.rules")
public record RuleProperties(Wash wash, Sp1 sp1) {
    public record Wash(long windowSeconds, BigDecimal qtyTolerancePct, BigDecimal priceTolerancePct,
                       String wt1Severity, String wt2Severity) {}
    public record Sp1(long windowSeconds, BigDecimal minPlacedQty, BigDecimal minCancelRatio,
                      long maxOrderLifetimeSeconds, String severity) {}
    public RuleProperties(Wash wash){this(wash,new Sp1(60,new BigDecimal("10000"),new BigDecimal("0.8"),5,"HIGH"));}
}
