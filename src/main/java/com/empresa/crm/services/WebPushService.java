package com.empresa.crm.services;

import com.empresa.crm.dto.PushMessageDTO;
import com.empresa.crm.entities.PushSubscription;

public interface WebPushService {
	void sendNotification(PushSubscription subscription, PushMessageDTO message);
}