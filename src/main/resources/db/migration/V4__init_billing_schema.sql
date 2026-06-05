CREATE SCHEMA IF NOT EXISTS billing;

CREATE TABLE billing.tariffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meter_type VARCHAR(30) NOT NULL,
    tariff_name VARCHAR(100) NOT NULL,
    rate DECIMAL(12,4) NOT NULL,
    fixed_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
    vat DECIMAL(5,2) NOT NULL DEFAULT 18,
    penalty_rate DECIMAL(5,2) NOT NULL DEFAULT 5,
    version INT NOT NULL,
    effective_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    UNIQUE(meter_type, version)
);

CREATE TABLE billing.bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    meter_id UUID NOT NULL,
    bill_number VARCHAR(50) NOT NULL UNIQUE,
    billing_month INT NOT NULL,
    billing_year INT NOT NULL,
    consumption DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    tax_amount DECIMAL(12,2) NOT NULL,
    penalty DECIMAL(12,2) NOT NULL DEFAULT 0,
    balance DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    generated_date TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE billing.pending_bill_generation (
    customer_id UUID NOT NULL,
    meter_id UUID NOT NULL,
    meter_type VARCHAR(30) NOT NULL,
    consumption DECIMAL(12,2) NOT NULL,
    billing_month INT NOT NULL,
    billing_year INT NOT NULL
);

-- Stored procedure: generate monthly bills
CREATE OR REPLACE PROCEDURE billing.generate_monthly_bills()
LANGUAGE plpgsql AS $$
DECLARE
    rec RECORD;
    t_rate DECIMAL; t_fixed DECIMAL; t_vat DECIMAL; t_penalty DECIMAL;
    v_amount DECIMAL; v_tax DECIMAL; v_total DECIMAL; v_bill_num VARCHAR;
BEGIN
    FOR rec IN SELECT * FROM billing.pending_bill_generation LOOP
        SELECT rate, fixed_charge, vat, penalty_rate INTO t_rate, t_fixed, t_vat, t_penalty
        FROM billing.tariffs WHERE meter_type = rec.meter_type AND active = TRUE
        ORDER BY version DESC LIMIT 1;
        IF t_rate IS NULL THEN CONTINUE; END IF;
        v_amount := (rec.consumption * t_rate) + t_fixed;
        v_tax := v_amount * (t_vat / 100);
        v_total := v_amount + v_tax;
        v_bill_num := 'BILL-' || TO_CHAR(NOW(), 'YYYYMM') || '-' || SUBSTRING(gen_random_uuid()::text, 1, 8);
        INSERT INTO billing.bills (customer_id, meter_id, bill_number, billing_month, billing_year,
            consumption, amount, tax_amount, penalty, balance, status, generated_date)
        VALUES (rec.customer_id, rec.meter_id, v_bill_num, rec.billing_month, rec.billing_year,
            rec.consumption, v_amount, v_tax, 0, v_total, 'PENDING', NOW());
    END LOOP;
    DELETE FROM billing.pending_bill_generation;
END;
$$;
