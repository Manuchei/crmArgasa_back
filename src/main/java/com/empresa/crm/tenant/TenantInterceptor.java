package com.empresa.crm.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class TenantInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

	    // ✅ Dejar pasar preflight
	    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
	        return true;
	    }

	    String tenant = request.getHeader("X-Empresa");

	    // ✅ fallback por query param (compatibilidad)
	    if (tenant == null || tenant.isBlank()) {
	        tenant = request.getParameter("empresa");
	    }

	    String path = request.getRequestURI();
	    boolean esPublico = path.contains("/api/usuarios/login") || path.contains("/api/usuarios/registro");

	    if (tenant == null || tenant.isBlank()) {
	        if (esPublico) return true;

	        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	        try {
	            response.getWriter().write("Empresa no seleccionada");
	        } catch (Exception ignored) {}
	        return false;
	    }

	    TenantContext.set(tenant.trim().toUpperCase());
	    return true;
	}


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
