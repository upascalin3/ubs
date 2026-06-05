package com.utility.billing.billing.scheduler;

import com.utility.billing.billing.service.BillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs at 01:00 on the 1st of each month to process queued readings
 * and generate bills via {@code billing.generate_monthly_bills()}.
 */
@Component
@ConditionalOnProperty(name = "app.billing.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MonthlyBillingScheduler {

	private static final Logger log = LoggerFactory.getLogger(MonthlyBillingScheduler.class);

	private final BillService billService;

	public MonthlyBillingScheduler(BillService billService) {
		this.billService = billService;
	}

	@Scheduled(cron = "${app.billing.scheduler.cron:0 0 1 1 * *}")
	public void generateMonthlyBills() {
		log.info("Starting scheduled monthly bill generation");
		billService.generateMonthlyBills();
		log.info("Scheduled monthly bill generation completed");
	}
}
