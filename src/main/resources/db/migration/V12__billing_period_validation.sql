-- Harden monthly bill generation: skip invalid periods, duplicates, and missing tariffs
CREATE OR REPLACE PROCEDURE billing.generate_monthly_bills()
LANGUAGE plpgsql AS $$
DECLARE
    rec RECORD;
    t_rate DECIMAL; t_fixed DECIMAL; t_vat DECIMAL;
    v_amount DECIMAL; v_tax DECIMAL; v_total DECIMAL; v_bill_num VARCHAR;
    v_period DATE;
    v_current_period DATE;
BEGIN
    v_current_period := date_trunc('month', CURRENT_DATE)::DATE;

    FOR rec IN
        SELECT p.*, m.installation_date AS install_date
        FROM billing.pending_bill_generation p
        JOIN auth.users u ON u.id = p.user_id
        JOIN customer.meters m ON m.id = p.meter_id
        WHERE u.status = 'ACTIVE'
          AND m.status = 'ACTIVE'
    LOOP
        v_period := make_date(rec.billing_year, rec.billing_month, 1);

        IF v_period > v_current_period THEN
            CONTINUE;
        END IF;

        IF rec.install_date IS NOT NULL
           AND v_period < date_trunc('month', rec.install_date)::DATE THEN
            CONTINUE;
        END IF;

        IF EXISTS (
            SELECT 1 FROM billing.bills b
            WHERE b.meter_id = rec.meter_id
              AND b.billing_month = rec.billing_month
              AND b.billing_year = rec.billing_year
        ) THEN
            CONTINUE;
        END IF;

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
    USING auth.users u, customer.meters m
    WHERE p.user_id = u.id
      AND p.meter_id = m.id
      AND u.status = 'ACTIVE'
      AND m.status = 'ACTIVE';
END;
$$;
