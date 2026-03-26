package com.empresa.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PushMessageDTO {
	private String title;
	private String body;
	private String url;
}