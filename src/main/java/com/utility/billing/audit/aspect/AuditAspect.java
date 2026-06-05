package com.utility.billing.audit.aspect;

import com.utility.billing.audit.dto.AuditLogRequest;
import com.utility.billing.audit.service.AuditLogService;
import com.utility.billing.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@org.springframework.context.annotation.Profile("!test")
public class AuditAspect {

	private final AuditLogService auditLogService;

	public AuditAspect(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@AfterReturning(
			pointcut = """
					execution(* com.utility.billing.auth.service.AuthService.login(..))
					|| execution(* com.utility.billing.auth.service.AuthService.register(..))
					|| execution(* com.utility.billing.auth.service.UserService.create(..))
					|| execution(* com.utility.billing.billing.service.BillService.generate(..))
					|| execution(* com.utility.billing.payment.service.PaymentService.record(..))
					""",
			returning = "result")
	public void logBusinessAction(JoinPoint joinPoint, Object result) {
		AuditLogRequest request = new AuditLogRequest();
		request.setUserId(SecurityUtils.getCurrentUserId());
		request.setAction(joinPoint.getSignature().getName());
		request.setEntityName(joinPoint.getTarget().getClass().getSimpleName());
		request.setIpAddress(resolveClientIp());
		request.setDetails(joinPoint.getSignature().toShortString());
		auditLogService.log(request);
	}

	private String resolveClientIp() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return null;
		}
		HttpServletRequest request = attrs.getRequest();
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
