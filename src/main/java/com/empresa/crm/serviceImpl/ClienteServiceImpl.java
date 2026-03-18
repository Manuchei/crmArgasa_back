package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.services.ClienteService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class ClienteServiceImpl implements ClienteService {

	private final ClienteRepository clienteRepository;

	public ClienteServiceImpl(ClienteRepository clienteRepository) {
		this.clienteRepository = clienteRepository;
	}

	private String empresaActual() {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}
		return empresa;
	}

	@Override
	public List<Cliente> findAll() {
		return clienteRepository.findByEmpresa(empresaActual());
	}

	@Override
	public Cliente findById(Long id) {
		return clienteRepository.findByIdAndEmpresa(id, empresaActual()).orElse(null);
	}

	@Override
	public Cliente save(Cliente cliente) {
		String empresa = empresaActual();

		if (cliente.getId() != null) {
			boolean existeEnEmpresa = clienteRepository.findByIdAndEmpresa(cliente.getId(), empresa).isPresent();
			if (!existeEnEmpresa) {
				throw new RuntimeException("No puedes modificar un cliente que no pertenece a la empresa actual.");
			}
		}

		cliente.setEmpresa(empresa);

		if (cliente.getNumeroCuenta() == null || cliente.getNumeroCuenta().isBlank()) {
			throw new RuntimeException("El número de cuenta es obligatorio.");
		}

		String numeroCuentaLimpio = cliente.getNumeroCuenta().replaceAll("\\s+", "").toUpperCase().trim();

		if (!numeroCuentaLimpio.matches("^ES\\d{22}$")) {
			throw new RuntimeException("El IBAN debe tener formato ES + 22 dígitos.");
		}

		cliente.setNumeroCuenta(numeroCuentaLimpio);

		// Normalización básica de direcciones
		cliente.setDireccion(trimOrNull(cliente.getDireccion()));
		cliente.setCodigoPostal(trimOrNull(cliente.getCodigoPostal()));
		cliente.setPoblacion(trimOrNull(cliente.getPoblacion()));
		cliente.setProvincia(trimOrNull(cliente.getProvincia()));

		cliente.setDireccionEntrega(trimOrNull(cliente.getDireccionEntrega()));
		cliente.setCodigoPostalEntrega(trimOrNull(cliente.getCodigoPostalEntrega()));
		cliente.setPoblacionEntrega(trimOrNull(cliente.getPoblacionEntrega()));
		cliente.setProvinciaEntrega(trimOrNull(cliente.getProvinciaEntrega()));

		if (cliente.getTrabajos() != null) {
			for (var t : cliente.getTrabajos()) {
				if (t == null)
					continue;
				t.setCliente(cliente);
				t.setEmpresa(empresa);
			}
		}

		if (cliente.getTotalImporte() == null) {
			cliente.setTotalImporte(0.0);
		}
		if (cliente.getTotalPagado() == null) {
			cliente.setTotalPagado(0.0);
		}

		return clienteRepository.save(cliente);
	}

	private String trimOrNull(String value) {
		if (value == null)
			return null;
		String limpio = value.trim();
		return limpio.isEmpty() ? null : limpio;
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		String empresa = empresaActual();

		Cliente cliente = clienteRepository.findByIdAndEmpresa(id, empresa)
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado o no pertenece a la empresa actual."));

		clienteRepository.delete(cliente);
	}
}