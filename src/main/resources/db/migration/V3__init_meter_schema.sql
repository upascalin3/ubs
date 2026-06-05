CREATE SCHEMA IF NOT EXISTS meter;
CREATE TABLE meter.meter_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meter_id UUID NOT NULL,
    previous_reading DECIMAL(12,2) NOT NULL,
    current_reading DECIMAL(12,2) NOT NULL,
    reading_date DATE NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    UNIQUE(meter_id, month, year)
);
