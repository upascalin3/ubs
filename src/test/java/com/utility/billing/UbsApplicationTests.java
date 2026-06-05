package com.utility.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UbsApplicationTests {

	@Test
	void applicationClassLoads() {
		assertNotNull(UbsApplication.class);
	}
}
