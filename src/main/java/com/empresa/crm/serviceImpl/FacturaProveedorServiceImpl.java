package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.FacturaProveedorRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.FacturaProveedorService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class FacturaProveedorServiceImpl implements FacturaProveedorService {

    private final FacturaProveedorRepository facturaRepo;
    private final ProveedorRepository proveedorRepo;
    private final TrabajoRepository trabajoRepo;

    public FacturaProveedorServiceImpl(FacturaProveedorRepository facturaRepo,
                                      ProveedorRepository proveedorRepo,
                                      TrabajoRepository trabajoRepo) {
        this.facturaRepo = facturaRepo;
        this.proveedorRepo = proveedorRepo;
        this.trabajoRepo = trabajoRepo;
    }

    @Override
    public List<FacturaProveedor> findAll() {
        String empresa = TenantContext.get();
        return facturaRepo.findByEmpresa(empresa);
    }

    @Override
    public FacturaProveedor findById(Long id) {
        String empresa = TenantContext.get();
        return facturaRepo.findByIdAndEmpresa(id, empresa).orElse(null);
    }

    @Override
    public FacturaProveedor generarFactura(Long proveedorId, String empresa) {
        String tenant = TenantContext.get();

        Proveedor proveedor = proveedorRepo.findByIdAndEmpresa(proveedorId, tenant).orElse(null);
        if (proveedor == null) return null;

        List<Trabajo> trabajosPendientes = trabajoRepo.findByProveedorId(proveedorId).stream()
                .filter(t -> !t.isPagado() && t.getFactura() == null)
                .collect(Collectors.toList());

        if (trabajosPendientes.isEmpty()) return null;

        double total = trabajosPendientes.stream()
                .mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0.0)
                .sum();

        FacturaProveedor factura = new FacturaProveedor();
        factura.setProveedor(proveedor);
        factura.setEmpresa(tenant); // ✅ forzado
        factura.setFechaEmision(LocalDate.now());
        factura.setPagada(false);
        factura.setTotalImporte(total);

        factura = facturaRepo.save(factura);

        for (Trabajo t : trabajosPendientes) {
            t.setFactura(factura);
            trabajoRepo.save(t);
        }

        return factura;
    }

    @Override
    public FacturaProveedor marcarComoPagada(Long facturaId) {
        String empresa = TenantContext.get();

        FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa).orElse(null);
        if (factura == null) return null;

        factura.setPagada(true);
        facturaRepo.save(factura);

        if (factura.getTrabajos() != null) {
            for (Trabajo t : factura.getTrabajos()) {
                t.setPagado(true);
                trabajoRepo.save(t);
            }
        }

        return factura;
    }

    @Override
    public List<FacturaProveedor> findByEmpresa(String empresa) {
        return facturaRepo.findByEmpresa(TenantContext.get());
    }

    @Override
    public List<FacturaProveedor> findByProveedor(Long proveedorId) {
        String empresa = TenantContext.get();
        return facturaRepo.findByProveedorIdAndEmpresa(proveedorId, empresa);
    }
}
