package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.services.ProveedorService;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public List<Proveedor> findAll() {
        return proveedorRepository.findAll();
    }

    @Override
    public Proveedor findById(Long id) {
        return proveedorRepository.findById(id).orElse(null);
    }

    @Override
    public Proveedor save(Proveedor proveedor) {

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
        proveedorRepository.deleteById(id);
    }

    @Override
    public List<Proveedor> findByOficio(String oficio) {
        return proveedorRepository.findByOficio(oficio);
    }

    @Override
    public List<Proveedor> findByEmpresa(String empresa) {
        if (empresa.equalsIgnoreCase("argasa")) {
            return proveedorRepository.findByTrabajaEnArgasaTrue();
        }
        if (empresa.equalsIgnoreCase("luga")) {
            return proveedorRepository.findByTrabajaEnLugaTrue();
        }
        return proveedorRepository.findAll();
    }

    // ⭐ NUEVO: BÚSQUEDA AVANZADA
    @Override
    public List<Proveedor> buscar(String texto, String empresa, String oficio) {

        return proveedorRepository.buscarAvanzado(
                texto == null ? "" : texto,
                empresa == null ? "" : empresa,
                oficio == null ? "" : oficio
        );
    }

}
