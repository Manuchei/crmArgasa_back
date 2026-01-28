package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.FacturaCliente;
import com.empresa.crm.entities.ServicioCliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.FacturaClienteRepository;
import com.empresa.crm.repositories.ServicioClienteRepository;
import com.empresa.crm.services.FacturaClienteService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class FacturaClienteServiceImpl implements FacturaClienteService {

    private final FacturaClienteRepository facturaRepo;
    private final ClienteRepository clienteRepo;
    private final ServicioClienteRepository servicioRepo;

    public FacturaClienteServiceImpl(FacturaClienteRepository facturaRepo,
                                     ClienteRepository clienteRepo,
                                     ServicioClienteRepository servicioRepo) {
        this.facturaRepo = facturaRepo;
        this.clienteRepo = clienteRepo;
        this.servicioRepo = servicioRepo;
    }

    @Override
    public List<FacturaCliente> findAll() {
        String empresa = TenantContext.get();
        return facturaRepo.findByEmpresa(empresa);
    }

    @Override
    public FacturaCliente findById(Long id) {
        String empresa = TenantContext.get();
        return facturaRepo.findByIdAndEmpresa(id, empresa).orElse(null);
    }

    @Override
    public FacturaCliente generarFactura(Long clienteId, String empresa) {

        // ✅ Tenant definitivo (viene validado desde el interceptor)
        final String tenant = (empresa == null) ? null : empresa.trim().toUpperCase();
        if (tenant == null || tenant.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa no seleccionada.");
        }

        // ✅ Cliente SOLO de esta empresa
        Cliente cliente = clienteRepo.findByIdAndEmpresa(clienteId, tenant).orElse(null);
        if (cliente == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cliente no existe en la empresa " + tenant + "."
            );
        }

        // ✅ Servicios por empresa (correctos)
        List<ServicioCliente> porEmpresa = servicioRepo.findByClienteIdAndEmpresa(clienteId, tenant);

        // ✅ Fallback: servicios antiguos con empresa NULL (para no “perder” trabajos viejos)
        List<ServicioCliente> sinEmpresa = servicioRepo.findByClienteIdAndEmpresaIsNull(clienteId);

        // ✅ Mezclar SIN duplicar (por id) manteniendo orden
        Map<Long, ServicioCliente> mapa = new LinkedHashMap<>();
        for (ServicioCliente s : porEmpresa) {
            if (s != null && s.getId() != null) mapa.put(s.getId(), s);
        }
        for (ServicioCliente s : sinEmpresa) {
            if (s != null && s.getId() != null) mapa.putIfAbsent(s.getId(), s);
        }

        List<ServicioCliente> servicios = mapa.values().stream().toList();

        // ✅ Solo los que NO tienen factura
        List<ServicioCliente> serviciosSinFactura = servicios.stream()
                .filter(s -> s.getFactura() == null)
                .toList();

        if (serviciosSinFactura.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No hay servicios sin factura para este cliente."
            );
        }

        // ✅ Total
        double total = serviciosSinFactura.stream()
                .mapToDouble(s -> s.getImporte() != null ? s.getImporte() : 0.0)
                .sum();

        // ✅ Crear factura
        FacturaCliente factura = new FacturaCliente();
        factura.setCliente(cliente);
        factura.setEmpresa(tenant);
        factura.setFechaEmision(LocalDate.now());
        factura.setPagada(false);
        factura.setTotalImporte(total);

        factura = facturaRepo.save(factura);

        // ✅ Vincular servicios y blindar empresa
        for (ServicioCliente s : serviciosSinFactura) {
            s.setEmpresa(tenant);
            s.setFactura(factura);
            servicioRepo.save(s);
        }

        return factura;
    }

    @Override
    public FacturaCliente marcarComoPagada(Long facturaId) {
        String empresa = TenantContext.get();

        FacturaCliente factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa).orElse(null);
        if (factura == null) return null;

        factura.setPagada(true);
        facturaRepo.save(factura);

        if (factura.getServicios() != null) {
            for (ServicioCliente s : factura.getServicios()) {
                s.setPagado(true);
                s.setEmpresa(empresa);
                servicioRepo.save(s);
            }
        }

        return factura;
    }

    @Override
    public List<FacturaCliente> findByEmpresa(String empresa) {
        return facturaRepo.findByEmpresa(TenantContext.get());
    }

    @Override
    public List<FacturaCliente> findByCliente(Long clienteId) {
        String empresa = TenantContext.get();
        return facturaRepo.findByClienteIdAndEmpresa(clienteId, empresa);
    }
}
