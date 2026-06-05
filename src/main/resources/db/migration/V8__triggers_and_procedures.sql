-- Bill insert: auto-create customer notification
CREATE OR REPLACE FUNCTION billing.fn_bill_notification()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notification.notifications (customer_id, title, message, status, created_at, updated_at)
    VALUES (
        NEW.customer_id,
        'New Bill Generated',
        'Bill ' || NEW.bill_number || ' for ' || NEW.billing_month || '/' || NEW.billing_year
            || ' — amount: ' || NEW.balance || ' RWF',
        'UNREAD',
        NOW(),
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bill_notification ON billing.bills;
CREATE TRIGGER trg_bill_notification
    AFTER INSERT ON billing.bills
    FOR EACH ROW EXECUTE FUNCTION billing.fn_bill_notification();

-- Payment insert: sync bill balance and mark PAID when balance reaches zero
CREATE OR REPLACE FUNCTION payment.fn_payment_completed()
RETURNS TRIGGER AS $$
DECLARE
    v_balance DECIMAL(12,2);
BEGIN
    UPDATE billing.bills
    SET balance = balance - NEW.amount_paid,
        updated_at = NOW()
    WHERE id = NEW.bill_id
    RETURNING balance INTO v_balance;

    IF v_balance IS NOT NULL AND v_balance <= 0 THEN
        UPDATE billing.bills
        SET balance = 0,
            status = 'PAID',
            updated_at = NOW()
        WHERE id = NEW.bill_id;

        INSERT INTO notification.notifications (customer_id, title, message, status, created_at, updated_at)
        SELECT NEW.customer_id,
               'Payment Received',
               'Bill fully paid. Reference: ' || NEW.reference_number,
               'UNREAD',
               NOW(),
               NOW();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payment_completed ON payment.payments;
CREATE TRIGGER trg_payment_completed
    AFTER INSERT ON payment.payments
    FOR EACH ROW EXECUTE FUNCTION payment.fn_payment_completed();
