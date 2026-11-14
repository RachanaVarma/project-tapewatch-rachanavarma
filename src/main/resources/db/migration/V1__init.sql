CREATE TABLE market_events (event_id VARCHAR(120) PRIMARY KEY, payload TEXT NOT NULL, event_time TIMESTAMPTZ NOT NULL);
CREATE TABLE alerts (
 id BIGSERIAL PRIMARY KEY, rule_id VARCHAR(20) NOT NULL, owner_token VARCHAR(40) NOT NULL,
 instrument VARCHAR(80) NOT NULL, severity VARCHAR(20) NOT NULL, status VARCHAR(32) NOT NULL,
 event_time TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL, evidence_event_ids VARCHAR(4000) NOT NULL
);
CREATE INDEX idx_alerts_list ON alerts (event_time DESC, rule_id, status, instrument);
