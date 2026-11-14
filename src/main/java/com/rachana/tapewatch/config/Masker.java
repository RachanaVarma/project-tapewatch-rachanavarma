package com.rachana.tapewatch.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class Masker {
    private final byte[] secret;
    public Masker(String secret) { this.secret = secret.getBytes(StandardCharsets.UTF_8); }
    public String token(String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return "OWN-" + HexFormat.of().formatHex(mac.doFinal(id.getBytes(StandardCharsets.UTF_8))).substring(0, 10);
        } catch (Exception e) { throw new IllegalStateException("Unable to mask identifier", e); }
    }
}
