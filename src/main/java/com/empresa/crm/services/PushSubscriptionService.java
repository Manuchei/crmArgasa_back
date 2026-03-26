package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.dto.PushSubscriptionRequestDTO;
import com.empresa.crm.entities.PushSubscription;

public interface PushSubscriptionService {
	PushSubscription saveSubscription(PushSubscriptionRequestDTO dto);

	void deleteByEndpoint(String endpoint);

	List<PushSubscription> getActiveByEmpresa(String empresa);
}