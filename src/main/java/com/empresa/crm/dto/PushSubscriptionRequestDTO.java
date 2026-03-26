package com.empresa.crm.dto;

import lombok.Data;

@Data
public class PushSubscriptionRequestDTO {
	private String empresa;
	private String endpoint;
	private KeysDTO keys;

	@Data
	public static class KeysDTO {
		private String p256dh;
		private String auth;
	}
}