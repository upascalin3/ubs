package com.utility.billing.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerRedirectController {

	@GetMapping({"/", "/docs"})
	public String redirectToSwagger() {
		return "redirect:/swagger-ui/index.html";
	}
}
