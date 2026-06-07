package com.utility.billing.notification.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * SRS notification and email body templates.
 */
public final class UtilityNotificationMessages {

	private UtilityNotificationMessages() {
	}

	public static String billGenerated(String customerName, int billingMonth, int billingYear, BigDecimal amount) {
		String name = customerName == null || customerName.isBlank() ? "Customer" : customerName;
		String period = formatPeriod(billingMonth, billingYear);
		return "Dear " + name + ",\nYour " + period
				+ " utility bill of " + formatAmount(amount) + " FRW has been successfully processed.";
	}

	/** Same SRS body used when a bill is fully paid (email + DB notification). */
	public static String fullPaymentProcessed(String customerName, int billingMonth, int billingYear,
			BigDecimal billTotal) {
		return billGenerated(customerName, billingMonth, billingYear, billTotal);
	}

	public static String partialPaymentReceived(String customerName, int billingMonth, int billingYear,
			BigDecimal amountPaid, BigDecimal remainingBalance) {
		String name = customerName == null || customerName.isBlank() ? "Customer" : customerName;
		String period = formatPeriod(billingMonth, billingYear);
		return "Dear " + name + ",\n"
				+ "Your partial payment of " + formatAmount(amountPaid) + " FRW for your " + period
				+ " utility bill has been received.\n"
				+ "Payment status: PARTIALLY_PAID\n"
				+ "Remaining balance: " + formatAmount(remainingBalance) + " FRW";
	}

	private static String formatPeriod(int billingMonth, int billingYear) {
		return Month.of(billingMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "/" + billingYear;
	}

	private static String formatAmount(BigDecimal amount) {
		if (amount == null) {
			return "0";
		}
		return amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
