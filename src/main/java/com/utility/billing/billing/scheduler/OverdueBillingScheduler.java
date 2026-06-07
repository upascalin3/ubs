package com.utility.billing.billing.scheduler;

import com.utility.billing.billing.service.BillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs daily to apply late payment penalties to bills past their due date.
 */
@Component
@ConditionalOnProperty(name = "app.billing.overdue-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OverdueBillingScheduler {

	private static final Logger log = LoggerFactory.getLogger(OverdueBillingScheduler.class);

	private final BillService billService;

	public OverdueBillingScheduler(BillService billService) {
		this.billService = billService;
	}

	@Scheduled(cron = "${app.billing.overdue-scheduler.cron:0 0 2 * * *}")
	public void applyOverduePenalties() {
		log.info("Starting scheduled overdue penalty processing");
		billService.applyOverduePenalties();
		log.info("Scheduled overdue penalty processing completed");
	}
}
