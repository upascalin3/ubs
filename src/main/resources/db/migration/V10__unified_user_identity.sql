-- SRS: ONE IDENTITY MODEL — User = Customer = Account Holder
-- Merge customer.customers into auth.users; replace customer_id with user_id

ALTER TABLE auth.users
    ADD COLUMN IF NOT EXISTS national_id VARCHAR(20),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_national_id
    ON auth.users (national_id) WHERE national_id IS NOT NULL;

-- Sync profile fields from legacy customers by email
UPDATE auth.users u
SET national_id = COALESCE(u.national_id, c.national_id),
    address = COALESCE(u.address, c.address)
FROM customer.customers c
WHERE LOWER(u.email) = LOWER(c.email);

-- Orphan customers → create users (account holders without login yet get inactive placeholder password)
INSERT INTO auth.users (full_name, email, phone_number, password, status, email_verified, national_id, address)
SELECT c.full_name,
       LOWER(c.email),
       c.phone,
       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       CASE WHEN c.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'INACTIVE' END,
       TRUE,
       c.national_id,
       c.address
FROM customer.customers c
WHERE NOT EXISTS (SELECT 1 FROM auth.users u WHERE LOWER(u.email) = LOWER(c.email));

-- Assign ROLE_CUSTOMER to newly created users from customers
INSERT INTO auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM auth.users u
JOIN customer.customers c ON LOWER(u.email) = LOWER(c.email)
JOIN auth.roles r ON r.role_name = 'ROLE_CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM auth.user_roles ur WHERE ur.user_id = u.id);

-- customer.meters: customer_id → user_id
ALTER TABLE customer.meters ADD COLUMN IF NOT EXISTS user_id UUID;

UPDATE customer.meters m
SET user_id = u.id
FROM customer.customers c
JOIN auth.users u ON LOWER(u.email) = LOWER(c.email)
WHERE m.customer_id = c.id AND m.user_id IS NULL;

ALTER TABLE customer.meters DROP COLUMN IF EXISTS customer_id;
ALTER TABLE customer.meters ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE customer.meters
    ADD CONSTRAINT fk_meters_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- billing.pending_bill_generation
ALTER TABLE billing.pending_bill_generation ADD COLUMN IF NOT EXISTS user_id UUID;

UPDATE billing.pending_bill_generation p
SET user_id = u.id
FROM customer.customers c
JOIN auth.users u ON LOWER(u.email) = LOWER(c.email)
WHERE p.customer_id = c.id AND p.user_id IS NULL;

ALTER TABLE billing.pending_bill_generation DROP COLUMN IF EXISTS customer_id;
ALTER TABLE billing.pending_bill_generation ALTER COLUMN user_id SET NOT NULL;

-- billing.bills
ALTER TABLE billing.bills ADD COLUMN IF NOT EXISTS user_id UUID;

UPDATE billing.bills b
SET user_id = u.id
FROM customer.customers c
JOIN auth.users u ON LOWER(u.email) = LOWER(c.email)
WHERE b.customer_id = c.id AND b.user_id IS NULL;

ALTER TABLE billing.bills DROP COLUMN IF EXISTS customer_id;
ALTER TABLE billing.bills ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE billing.bills
    ADD CONSTRAINT fk_bills_user FOREIGN KEY (user_id) REFERENCES auth.users(id);

-- notification.notifications
ALTER TABLE notification.notifications ADD COLUMN IF NOT EXISTS user_id UUID;

UPDATE notification.notifications n
SET user_id = u.id
FROM customer.customers c
JOIN auth.users u ON LOWER(u.email) = LOWER(c.email)
WHERE n.customer_id = c.id AND n.user_id IS NULL;

ALTER TABLE notification.notifications DROP COLUMN IF EXISTS customer_id;
ALTER TABLE notification.notifications ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE notification.notifications
    ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES auth.users(id);

-- payment.payments: SRS — bill_id only
ALTER TABLE payment.payments DROP COLUMN IF EXISTS customer_id;

-- Drop legacy customer identity table
DROP TABLE IF EXISTS customer.customers CASCADE;

-- Updated monthly bill generation
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
    DELETE FROM billing.pending_bill_generation;
END;
$$;

CREATE OR REPLACE FUNCTION billing.fn_bill_notification()
RETURNS TRIGGER AS $$
DECLARE
    v_name VARCHAR;
    v_period TEXT;
BEGIN
    SELECT full_name INTO v_name FROM auth.users WHERE id = NEW.user_id;
    v_period := TO_CHAR(make_date(NEW.billing_year, NEW.billing_month, 1), 'FMMonth YYYY');

    INSERT INTO notification.notifications (user_id, title, message, status, created_at, updated_at)
    VALUES (
        NEW.user_id,
        'New Bill Generated',
        'Dear ' || COALESCE(v_name, 'Customer') || E',\nYour ' || v_period
            || ' utility bill of ' || NEW.balance || ' FRW has been successfully processed.',
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
BEGIN
    UPDATE billing.bills
    SET balance = balance - NEW.amount_paid,
        updated_at = NOW()
    WHERE id = NEW.bill_id
    RETURNING balance, user_id INTO v_balance, v_user_id;

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

        INSERT INTO notification.notifications (user_id, title, message, status, created_at, updated_at)
        VALUES (
            v_user_id,
            'Payment Received',
            'Dear ' || COALESCE(v_name, 'Customer') || E',\nYour payment has been received.\n'
                || 'Your utility bill is fully paid.',
            'SENT',
            NOW(),
            NOW()
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
