package com.utility.billing.notification.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilityNotificationMessagesTest {

	@Test
	void billGeneratedMatchesSrsFormat() {
		String message = UtilityNotificationMessages.billGenerated(
				"Pascaline Uwayo", 6, 2026, new BigDecimal("15930.00"));
		assertEquals(
				"Dear Pascaline Uwayo,\nYour June/2026 utility bill of 15930 FRW has been successfully processed.",
				message);
	}

	@Test
	void fullPaymentUsesSrsFormat() {
		String message = UtilityNotificationMessages.fullPaymentProcessed(
				"John Doe", 6, 2026, new BigDecimal("15930.00"));
		assertEquals(
				"Dear John Doe,\nYour June/2026 utility bill of 15930 FRW has been successfully processed.",
				message);
	}
}
