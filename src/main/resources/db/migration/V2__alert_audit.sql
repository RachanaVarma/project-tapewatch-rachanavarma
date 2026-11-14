CREATE TABLE alert_audit (
 id BIGSERIAL PRIMARY KEY, alert_id BIGINT NOT NULL REFERENCES alerts(id), acting_user VARCHAR(120) NOT NULL,
 from_status VARCHAR(32) NOT NULL, to_status VARCHAR(32) NOT NULL, note VARCHAR(2000) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_alert_audit_history ON alert_audit (alert_id, created_at);
