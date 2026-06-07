-- Full payment: update bill status + SRS-style notification (DB trigger)
-- Email is sent from Java PaymentService after commit.

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
        v_amount := TRIM(TRAILING '0' FROM TRIM(TRAILING '.' FROM v_total::TEXT));

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
