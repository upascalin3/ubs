package com.utility.billing.billing.util;

import com.utility.billing.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingPeriodValidatorTest {

	@Test
	void acceptsPastInstallationDate() {
		assertDoesNotThrow(() -> BillingPeriodValidator.validateInstallationDate(LocalDate.of(2020, 1, 15)));
	}

	@Test
	void rejectsFutureInstallationDate() {
		assertThrows(BusinessException.class,
				() -> BillingPeriodValidator.validateInstallationDate(LocalDate.now().plusDays(1)));
	}

	@Test
	void rejectsFutureBillingPeriod() {
		LocalDate future = LocalDate.now().plusMonths(2);
		assertThrows(BusinessException.class,
				() -> BillingPeriodValidator.validateNotFuturePeriod(future.getMonthValue(), future.getYear()));
	}

	@Test
	void rejectsBillingPeriodBeforeInstallation() {
		assertThrows(BusinessException.class, () -> BillingPeriodValidator.validateNotBeforeInstallation(
				LocalDate.of(2026, 6, 15), 5, 2026));
	}

	@Test
	void rejectsMismatchedReadingMonthYear() {
		assertThrows(BusinessException.class, () -> BillingPeriodValidator.validateReadingDateInPeriod(
				LocalDate.of(2026, 6, 5), 5, 2026));
	}

	@Test
	void acceptsMatchingReadingMonthYear() {
		assertDoesNotThrow(() -> {
			BillingPeriodValidator.validateReadingDateInPeriod(LocalDate.of(2026, 6, 5), 6, 2026);
			BillingPeriodValidator.validateNotFuturePeriod(6, 2026);
		});
	}
}
