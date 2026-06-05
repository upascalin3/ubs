CREATE SCHEMA IF NOT EXISTS audit;
CREATE TABLE audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id UUID,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),
    details TEXT
);
