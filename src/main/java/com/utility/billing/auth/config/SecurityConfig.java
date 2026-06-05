package com.utility.billing.auth.config;

import com.utility.billing.auth.repository.TokenBlacklistRepository;
import com.utility.billing.common.security.JwtAuthenticationFilter;
import com.utility.billing.common.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
			TokenBlacklistRepository tokenBlacklistRepository) {
		return new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistRepository::existsByToken);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationFilter jwtFilter) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/register", "/api/auth/signup", "/api/auth/login",
								"/api/auth/verify-otp", "/api/auth/refresh",
								"/api/auth/forgot-password", "/api/auth/verify-reset-otp",
								"/api/auth/reset-password",
								"/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html",
								"/api-docs/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**",
								"/api/docs/**",
								"/actuator/health",
								"/", "/docs")
						.permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
