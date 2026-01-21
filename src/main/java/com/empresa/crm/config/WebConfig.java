package com.empresa.crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
		  .allowedOrigins("http://localhost:4200")
		  .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
		  .allowedHeaders("*")
		  .allowCredentials(true);

	}

	/*
	 * @Override public void extendMessageConverters(List<HttpMessageConverter<?>>
	 * converters) {
	 * 
	 * MediaType jsonUtf8 =
	 * MediaType.parseMediaType("application/json;charset=UTF-8");
	 * 
	 * for (HttpMessageConverter<?> c : converters) { if (c instanceof
	 * MappingJackson2HttpMessageConverter jackson) {
	 * 
	 * // ✅ Copia mutable (evita UnsupportedOperationException) List<MediaType>
	 * mediaTypes = new ArrayList<>(jackson.getSupportedMediaTypes());
	 * 
	 * if (!mediaTypes.contains(jsonUtf8)) { mediaTypes.add(jsonUtf8);
	 * jackson.setSupportedMediaTypes(mediaTypes); } } } }
	 */
}
