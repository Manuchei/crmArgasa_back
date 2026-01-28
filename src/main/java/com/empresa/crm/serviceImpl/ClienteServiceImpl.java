package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.services.ClienteService;
import com.empresa.crm.tenant.TenantContext;
import org.springframework.transaction.annotation.Transactional;


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

        // ✅ Seguridad multi-tenant: si es update, comprobar que el id pertenece a la empresa actual
        if (cliente.getId() != null) {
            boolean existeEnEmpresa = clienteRepository.findByIdAndEmpresa(cliente.getId(), empresa).isPresent();
            if (!existeEnEmpresa) {
                throw new RuntimeException("No puedes modificar un cliente que no pertenece a la empresa actual.");
            }
        }

        // ✅ Forzar empresa desde backend (no confiar en frontend)
        cliente.setEmpresa(empresa);

        // ✅ IMPORTANTÍSIMO: Propagar empresa y relación a los trabajos (para que no vaya NULL)
        if (cliente.getTrabajos() != null) {
            for (var t : cliente.getTrabajos()) {
                if (t == null) continue;
                t.setCliente(cliente);  // asegura la FK cliente_id
                t.setEmpresa(empresa);  // asegura NOT NULL en trabajos.empresa
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


    @Override
    @Transactional
    public void deleteById(Long id) {
        String empresa = empresaActual();

        Cliente cliente = clienteRepository.findByIdAndEmpresa(id, empresa)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado o no pertenece a la empresa actual."));

        // Esto SÍ dispara cascade = ALL y orphanRemoval = true hacia trabajos
        clienteRepository.delete(cliente);
    }

}
