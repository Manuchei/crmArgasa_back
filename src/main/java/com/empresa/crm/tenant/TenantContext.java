package com.empresa.crm.tenant;

public class TenantContext {
	private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

	public static void set(String tenant) {
		CURRENT.set(tenant);
	}

	public static String get() {
		return CURRENT.get();
	}

	public static void clear() {
		CURRENT.remove();
	}
}
