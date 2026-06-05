package com.utility.billing.common.security;

import com.utility.billing.common.config.SecurityAuditor;
import com.utility.billing.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final Predicate<String> tokenBlacklistChecker;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this(jwtTokenProvider, token -> false);
	}

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, Predicate<String> tokenBlacklistChecker) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.tokenBlacklistChecker = tokenBlacklistChecker;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);
		if (StringUtils.hasText(token)) {
			if (tokenBlacklistChecker.test(token)) {
				throw new UnauthorizedException("Token has been revoked");
			}
			jwtTokenProvider.validateToken(token);
			UUID userId = jwtTokenProvider.getUserId(token);
			List<String> roles = jwtTokenProvider.getRoles(token);
			var authorities = roles.stream()
					.map(SimpleGrantedAuthority::new)
					.collect(Collectors.toList());
			var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(auth);
			SecurityAuditor.setCurrentUserId(userId);
		}
		try {
			filterChain.doFilter(request, response);
		} finally {
			SecurityAuditor.clear();
		}
	}

	private String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}
		return null;
	}
}
