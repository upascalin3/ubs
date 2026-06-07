-- Align bill notification message with SRS format:
-- Dear <CustomerName>,
-- Your <Month/Year> utility bill of <Amount> FRW has been successfully processed.

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

    v_amount := TRIM(TRAILING '0' FROM TRIM(TRAILING '.' FROM NEW.balance::TEXT));

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
