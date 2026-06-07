package com.utility.billing.billing.util;

import com.utility.billing.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Validates billing and reading periods (month/year) against calendar rules,
 * meter installation dates, and the current date.
 */
public final class BillingPeriodValidator {

	public static final int MIN_YEAR = 2000;

	private BillingPeriodValidator() {
	}

	public static YearMonth toPeriod(int month, int year) {
		validateMonthYear(month, year);
		return YearMonth.of(year, month);
	}

	public static void validateMonthYear(int month, int year) {
		if (month < 1 || month > 12) {
			throw new BusinessException("Billing month must be between 1 and 12");
		}
		if (year < MIN_YEAR) {
			throw new BusinessException("Billing year must be " + MIN_YEAR + " or later");
		}
		if (year > YearMonth.now().getYear() + 1) {
			throw new BusinessException("Billing year cannot be more than one year ahead of the current year");
		}
	}

	public static void validateNotFuturePeriod(int month, int year) {
		YearMonth period = toPeriod(month, year);
		YearMonth current = YearMonth.now();
		if (period.isAfter(current)) {
			throw new BusinessException(
					"Billing period " + formatPeriod(period) + " cannot be in the future");
		}
	}

	public static void validateNotBeforeInstallation(LocalDate installationDate, int month, int year) {
		if (installationDate == null) {
			return;
		}
		YearMonth period = toPeriod(month, year);
		YearMonth installPeriod = YearMonth.from(installationDate);
		if (period.isBefore(installPeriod)) {
			throw new BusinessException(
					"Billing period " + formatPeriod(period)
							+ " cannot be before meter installation date (" + installationDate + ")");
		}
	}

	public static void validateReadingDateInPeriod(LocalDate readingDate, int month, int year) {
		if (readingDate == null) {
			return;
		}
		YearMonth period = toPeriod(month, year);
		YearMonth fromDate = YearMonth.from(readingDate);
		if (!fromDate.equals(period)) {
			throw new BusinessException(
					"Reading month/year (" + month + "/" + year
							+ ") must match reading date (" + readingDate + ")");
		}
	}

	public static void validateReadingDateNotFuture(LocalDate readingDate) {
		if (readingDate != null && readingDate.isAfter(LocalDate.now())) {
			throw new BusinessException("Reading date cannot be in the future");
		}
	}

	public static void validateInstallationDate(LocalDate installationDate) {
		if (installationDate == null) {
			throw new BusinessException("Installation date is required");
		}
		if (installationDate.isAfter(LocalDate.now())) {
			throw new BusinessException("Installation date cannot be in the future");
		}
	}

	private static String formatPeriod(YearMonth period) {
		return period.getMonthValue() + "/" + period.getYear();
	}
}
