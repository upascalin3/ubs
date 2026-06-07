-- Fix amount formatting in notification messages (avoid trailing decimal e.g. "15930.")

CREATE OR REPLACE FUNCTION billing.fn_bill_notification()
RETURNS TRIGGER AS $$
DECLARE
    v_name VARCHAR;
    v_period TEXT;
    v_amount TEXT;
BEGIN
    SELECT full_name INTO v_name FROM auth.users WHERE id = NEW.user_id;

    v_period := TRIM(TO_CHAR(make_date(NEW.billing_year, NEW.billing_month, 1), 'FMMonth'))
        || '/' || NEW.billing_year;

    v_amount := regexp_replace(trim(to_char(NEW.balance, 'FM999999999990.00')), '\.?0+$', '');

    INSERT INTO notification.notifications (user_id, title, message, status, created_at, updated_at)
    VALUES (
        NEW.user_id,
        'New Bill Generated',
        'Dear ' || COALESCE(v_name, 'Customer') || E',\nYour ' || v_period
            || ' utility bill of ' || v_amount || ' FRW has been successfully processed.',
        'SENT',
        NOW(),
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION payment.fn_payment_completed()
RETURNS TRIGGER AS $$
DECLARE
    v_balance DECIMAL(12,2);
    v_user_id UUID;
    v_name VARCHAR;
    v_month INT;
    v_year INT;
    v_total DECIMAL(12,2);
    v_period TEXT;
    v_amount TEXT;
BEGIN
    UPDATE billing.bills
    SET balance = balance - NEW.amount_paid,
        updated_at = NOW()
    WHERE id = NEW.bill_id
    RETURNING balance, user_id, billing_month, billing_year,
              (amount + tax_amount + penalty) INTO
        v_balance, v_user_id, v_month, v_year, v_total;

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

        SELECT full_name INTO v_name FROM auth.users WHERE id = v_user_id;

        v_period := TRIM(TO_CHAR(make_date(v_year, v_month, 1), 'FMMonth')) || '/' || v_year;
        v_amount := regexp_replace(trim(to_char(v_total, 'FM999999999990.00')), '\.?0+$', '');

        INSERT INTO notification.notifications (user_id, title, message, status, created_at, updated_at)
        VALUES (
            v_user_id,
            'Payment Received',
            'Dear ' || COALESCE(v_name, 'Customer') || E',\nYour ' || v_period
                || ' utility bill of ' || v_amount || ' FRW has been successfully processed.',
            'SENT',
            NOW(),
            NOW()
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
