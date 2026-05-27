package com.empresa.crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.empresa.crm.tenant.TenantInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		registry.addInterceptor(new TenantInterceptor()).addPathPatterns("/api/**")
				.excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/push/**");
	}
}