-- Tariff selection by effective date for billing period
CREATE OR REPLACE PROCEDURE billing.generate_monthly_bills()
LANGUAGE plpgsql AS $$
DECLARE
    rec RECORD;
    t_rate DECIMAL; t_fixed DECIMAL; t_vat DECIMAL;
    v_amount DECIMAL; v_tax DECIMAL; v_total DECIMAL; v_bill_num VARCHAR;
    v_period DATE;
BEGIN
    FOR rec IN SELECT * FROM billing.pending_bill_generation LOOP
        v_period := make_date(rec.billing_year, rec.billing_month, 1);

        SELECT rate, fixed_charge, vat INTO t_rate, t_fixed, t_vat
        FROM billing.tariffs
        WHERE meter_type = rec.meter_type
          AND active = TRUE
          AND effective_date <= v_period
        ORDER BY version DESC
        LIMIT 1;

        IF t_rate IS NULL THEN CONTINUE; END IF;

        -- (Consumption × Tariff) + ServiceCharge; VAT on subtotal
        v_amount := (rec.consumption * t_rate) + t_fixed;
        v_tax := v_amount * (t_vat / 100);
        v_total := v_amount + v_tax;
        v_bill_num := 'BILL-' || rec.billing_year || LPAD(rec.billing_month::text, 2, '0')
            || '-' || UPPER(SUBSTRING(gen_random_uuid()::text, 1, 8));

        INSERT INTO billing.bills (customer_id, meter_id, bill_number, billing_month, billing_year,
            consumption, amount, tax_amount, penalty, balance, status, generated_date)
        VALUES (rec.customer_id, rec.meter_id, v_bill_num, rec.billing_month, rec.billing_year,
            rec.consumption, v_amount, v_tax, 0, v_total, 'PENDING', NOW());
    END LOOP;
    DELETE FROM billing.pending_bill_generation;
END;
$$;

-- Personalized bill notification (trigger on bill insert)
CREATE OR REPLACE FUNCTION billing.fn_bill_notification()
RETURNS TRIGGER AS $$
DECLARE
    v_name VARCHAR;
    v_period TEXT;
BEGIN
    SELECT full_name INTO v_name FROM customer.customers WHERE id = NEW.customer_id;
    v_period := TO_CHAR(make_date(NEW.billing_year, NEW.billing_month, 1), 'FMMonth YYYY');

    INSERT INTO notification.notifications (customer_id, title, message, status, created_at, updated_at)
    VALUES (
        NEW.customer_id,
        'New Bill Generated',
        'Dear ' || COALESCE(v_name, 'Customer') || E',\nYour ' || v_period
            || ' utility bill of ' || NEW.balance || ' FRW has been successfully processed.',
        'UNREAD',
        NOW(),
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Payment trigger: PARTIALLY_PAID or PAID + personalized notification
CREATE OR REPLACE FUNCTION payment.fn_payment_completed()
RETURNS TRIGGER AS $$
DECLARE
    v_balance DECIMAL(12,2);
    v_name VARCHAR;
BEGIN
    UPDATE billing.bills
    SET balance = balance - NEW.amount_paid,
        updated_at = NOW()
    WHERE id = NEW.bill_id
    RETURNING balance INTO v_balance;

    IF v_balance IS NULL THEN
        RETURN NEW;
    END IF;

    IF v_balance > 0 THEN
        UPDATE billing.bills
        SET status = 'PARTIALLY_PAID',
            updated_at = NOW()
        WHERE id = NEW.bill_id;
    ELSE
        UPDATE billing.bills
        SET balance = 0,
            status = 'PAID',
            updated_at = NOW()
        WHERE id = NEW.bill_id;

        SELECT full_name INTO v_name FROM customer.customers WHERE id = NEW.customer_id;

        INSERT INTO notification.notifications (customer_id, title, message, status, created_at, updated_at)
        VALUES (
            NEW.customer_id,
            'Payment Received',
            'Dear ' || COALESCE(v_name, 'Customer') || E',\nYour payment has been received.\n'
                || 'Your utility bill is fully paid.',
            'UNREAD',
            NOW(),
            NOW()
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
