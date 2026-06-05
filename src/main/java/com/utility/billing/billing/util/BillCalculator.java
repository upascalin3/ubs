package com.utility.billing.billing.util;

import com.utility.billing.billing.entity.Tariff;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Bill = (Consumption × Tariff) + ServiceCharge + VAT
 * VAT is applied on (usage cost + service charge).
 */
public final class BillCalculator {

	private BillCalculator() {
	}

	public record BillAmounts(BigDecimal usageCost, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total) {
	}

	public static BillAmounts calculate(BigDecimal consumption, Tariff tariff) {
		BigDecimal usageCost = consumption.multiply(tariff.getRate()).setScale(2, RoundingMode.HALF_UP);
		BigDecimal subtotal = usageCost.add(tariff.getFixedCharge()).setScale(2, RoundingMode.HALF_UP);
		BigDecimal taxAmount = subtotal.multiply(tariff.getVat().divide(BigDecimal.valueOf(100)))
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
		return new BillAmounts(usageCost, subtotal, taxAmount, total);
	}
}
