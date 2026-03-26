package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.PushSubscriptionRequestDTO;
import com.empresa.crm.entities.PushSubscription;
import com.empresa.crm.repositories.PushSubscriptionRepository;
import com.empresa.crm.services.PushSubscriptionService;

@Service
public class PushSubscriptionServiceImpl implements PushSubscriptionService {

	private final PushSubscriptionRepository repository;

	public PushSubscriptionServiceImpl(PushSubscriptionRepository repository) {
		this.repository = repository;
	}

	@Override
	public PushSubscription saveSubscription(PushSubscriptionRequestDTO dto) {
		String empresa = dto.getEmpresa().trim().toUpperCase();

		PushSubscription sub = repository.findByEndpoint(dto.getEndpoint()).orElseGet(PushSubscription::new);

		sub.setEmpresa(empresa);
		sub.setEndpoint(dto.getEndpoint());
		sub.setP256dh(dto.getKeys().getP256dh());
		sub.setAuth(dto.getKeys().getAuth());
		sub.setActiva(true);

		return repository.save(sub);
	}

	@Override
	public void deleteByEndpoint(String endpoint) {
		repository.findByEndpoint(endpoint).ifPresent(repository::delete);
	}

	@Override
	public List<PushSubscription> getActiveByEmpresa(String empresa) {
		return repository.findByEmpresaAndActivaTrue(empresa.trim().toUpperCase());
	}
}