CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount_paid DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_date DATE NOT NULL,
    reference_number VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

CREATE TABLE payment.bill_balances (
    bill_id UUID PRIMARY KEY,
    balance DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE OR REPLACE FUNCTION payment.fn_payment_completed()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE payment.bill_balances SET balance = balance - NEW.amount_paid
    WHERE bill_id = NEW.bill_id;
    UPDATE payment.bill_balances SET status = 'PAID'
    WHERE bill_id = NEW.bill_id AND balance <= 0;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_completed
AFTER INSERT ON payment.payments
FOR EACH ROW EXECUTE FUNCTION payment.fn_payment_completed();
