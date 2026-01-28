package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.services.ProveedorService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public List<Proveedor> findAll() {
        String empresa = TenantContext.get();
        return proveedorRepository.findByEmpresa(empresa);
    }

    @Override
    public Proveedor findById(Long id) {
        String empresa = TenantContext.get();
        return proveedorRepository.findByIdAndEmpresa(id, empresa).orElse(null);
    }

    @Override
    public Proveedor save(Proveedor proveedor) {
        String empresa = TenantContext.get();

        // ✅ Forzar empresa desde backend
        proveedor.setEmpresa(empresa);

        double total = 0.0;
        double pagado = 0.0;

        if (proveedor.getTrabajos() != null) {
            for (var t : proveedor.getTrabajos()) {
                total += t.getImporte() != null ? t.getImporte() : 0;
                pagado += t.getImportePagado() != null ? t.getImportePagado() : 0;
            }
        }

        proveedor.setImporteTotal(total);
        proveedor.setImportePagado(pagado);
        proveedor.setImportePendiente(total - pagado);

        return proveedorRepository.save(proveedor);
    }

    @Override
    public void deleteById(Long id) {
        String empresa = TenantContext.get();
        proveedorRepository.deleteByIdAndEmpresa(id, empresa);
    }

    @Override
    public List<Proveedor> findByOficio(String oficio) {
        String empresa = TenantContext.get();
        return proveedorRepository.findByEmpresaAndOficio(empresa, oficio);
    }

    // ⚠️ Este método ya no debería recibir empresa desde fuera, pero lo mantengo por compatibilidad
    @Override
    public List<Proveedor> findByEmpresa(String empresa) {
        // ✅ Ignoramos el parámetro y usamos TenantContext
        return proveedorRepository.findByEmpresa(TenantContext.get());
    }

    @Override
    public List<Proveedor> buscar(String texto, String empresa, String oficio) {
        String tenant = TenantContext.get();

        return proveedorRepository.buscarAvanzado(
                texto == null ? "" : texto,
                tenant, // ✅ forzado
                oficio == null ? "" : oficio
        );
    }
}
