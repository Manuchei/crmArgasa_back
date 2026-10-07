package com.empresa.crm.security;

import com.empresa.crm.services.RegistroActividadService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class ActividadConfig implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(ActividadConfig.class);

	private final RegistroActividadService auditoria;

	public ActividadConfig(RegistroActividadService auditoria) {
		this.auditoria = auditoria;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {

		registry.addInterceptor(new HandlerInterceptor() {

			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

				if (request.getUserPrincipal() != null) {

					request.setAttribute("audit.actor", request.getUserPrincipal().getName());

					String empresa = request.getHeader("X-Empresa");

					if (empresa == null || empresa.isBlank()) {
						empresa = request.getParameter("empresa");
					}

					request.setAttribute("audit.empresa", empresa);
				}

				return true;
			}

			@Override
			public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
					Exception ex) {

				if ("OPTIONS".equalsIgnoreCase(request.getMethod())
						|| "/api/auth/login".equals(request.getRequestURI())) {
					return;
				}

				String actor = (String) request.getAttribute("audit.actor");

				if (actor == null) {
					return;
				}

				int estado = response.getStatus();

				if (ex != null && estado < 400) {
					estado = 500;
				}

				try {
					auditoria.registrar(actor, "USO_API", request.getMethod() + " " + request.getRequestURI(), estado,
							(String) request.getAttribute("audit.empresa"));
				} catch (Exception error) {
					// La petición ya terminó; avisar si no se pudo guardar.
					log.error("No se pudo guardar el registro de uso", error);
				}
			}

		}).addPathPatterns("/api/**");
	}
}