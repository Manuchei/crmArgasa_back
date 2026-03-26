package com.empresa.crm.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String empresa;

	@Column(nullable = false, columnDefinition = "TEXT", unique = true)
	private String endpoint;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String p256dh;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String auth;

	@Column(nullable = false)
	private boolean activa = true;
}