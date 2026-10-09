package com.empresa.crm.tenant;

import java.util.Locale;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TenantInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

		TenantContext.clear();

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		String path = request.getServletPath();

		// La empresa del webhook se comprueba en su contenido firmado.
		if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/webhooks/facturadirecta".equals(path)) {
			return true;
		}

		boolean esPublico = "/api/auth/login".equals(path) || "/api/auth/register".equals(path);

		String tenant = request.getHeader("X-Empresa");

		if (tenant == null || tenant.isBlank()) {
			tenant = request.getParameter("empresa");
		}

		if (tenant == null || tenant.isBlank()) {
			if (esPublico) {
				return true;
			}

			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("text/plain;charset=UTF-8");

			try {
				response.getWriter().write("Empresa no seleccionada");
			} catch (java.io.IOException ignored) {
			}

			return false;
		}

		TenantContext.set(tenant.trim().toUpperCase(Locale.ROOT));
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {

		TenantContext.clear();
	}
}