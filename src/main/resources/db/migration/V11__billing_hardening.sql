-- Skip inactive users during automated bill generation
CREATE OR REPLACE PROCEDURE billing.generate_monthly_bills()
LANGUAGE plpgsql AS $$
DECLARE
    rec RECORD;
    t_rate DECIMAL; t_fixed DECIMAL; t_vat DECIMAL;
    v_amount DECIMAL; v_tax DECIMAL; v_total DECIMAL; v_bill_num VARCHAR;
    v_period DATE;
BEGIN
    FOR rec IN
        SELECT p.*
        FROM billing.pending_bill_generation p
        JOIN auth.users u ON u.id = p.user_id
        WHERE u.status = 'ACTIVE'
    LOOP
        v_period := make_date(rec.billing_year, rec.billing_month, 1);

        SELECT rate, fixed_charge, vat INTO t_rate, t_fixed, t_vat
        FROM billing.tariffs
        WHERE meter_type = rec.meter_type
          AND active = TRUE
          AND effective_date <= v_period
        ORDER BY version DESC
        LIMIT 1;

        IF t_rate IS NULL THEN CONTINUE; END IF;

        v_amount := (rec.consumption * t_rate) + t_fixed;
        v_tax := v_amount * (t_vat / 100);
        v_total := v_amount + v_tax;
        v_bill_num := 'BILL-' || rec.billing_year || LPAD(rec.billing_month::text, 2, '0')
            || '-' || UPPER(SUBSTRING(gen_random_uuid()::text, 1, 8));

        INSERT INTO billing.bills (user_id, meter_id, bill_number, billing_month, billing_year,
            consumption, amount, tax_amount, penalty, balance, status, generated_date)
        VALUES (rec.user_id, rec.meter_id, v_bill_num, rec.billing_month, rec.billing_year,
            rec.consumption, v_amount, v_tax, 0, v_total, 'PENDING', NOW());
    END LOOP;

    DELETE FROM billing.pending_bill_generation p
    USING auth.users u
    WHERE p.user_id = u.id AND u.status = 'ACTIVE';
END;
$$;

ALTER TABLE billing.bills ADD COLUMN IF NOT EXISTS due_date DATE;

-- Apply late payment penalties to overdue bills
CREATE OR REPLACE PROCEDURE billing.apply_overdue_penalties()
LANGUAGE plpgsql AS $$
DECLARE
    rec RECORD;
    v_penalty DECIMAL(12,2);
    v_penalty_rate DECIMAL(5,2);
    v_meter_type VARCHAR(30);
    v_period DATE;
BEGIN
    FOR rec IN
        SELECT b.*
        FROM billing.bills b
        WHERE b.status IN ('APPROVED', 'PARTIALLY_PAID')
          AND b.due_date IS NOT NULL
          AND b.due_date < CURRENT_DATE
    LOOP
        SELECT m.meter_type INTO v_meter_type
        FROM customer.meters m
        WHERE m.id = rec.meter_id;

        v_period := make_date(rec.billing_year, rec.billing_month, 1);

        SELECT penalty_rate INTO v_penalty_rate
        FROM billing.tariffs
        WHERE meter_type = v_meter_type
          AND active = TRUE
          AND effective_date <= v_period
        ORDER BY version DESC
        LIMIT 1;

        v_penalty_rate := COALESCE(v_penalty_rate, 0);
        v_penalty := ROUND(rec.balance * (v_penalty_rate / 100), 2);

        UPDATE billing.bills
        SET penalty = penalty + v_penalty,
            balance = balance + v_penalty,
            status = 'OVERDUE',
            updated_at = NOW()
        WHERE id = rec.id;
    END LOOP;
END;
$$;
