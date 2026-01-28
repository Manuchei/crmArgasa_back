package com.empresa.crm.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.AlbaranCliente;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.LineaAlbaranCliente;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.AlbaranClienteRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.TrabajoRepository;

@Service
public class AlbaranClienteService {

    private final AlbaranClienteRepository albaranRepo;
    private final ClienteRepository clienteRepo;
    private final TrabajoRepository trabajoRepo;

    public AlbaranClienteService(AlbaranClienteRepository albaranRepo,
                                 ClienteRepository clienteRepo,
                                 TrabajoRepository trabajoRepo) {
        this.albaranRepo = albaranRepo;
        this.clienteRepo = clienteRepo;
        this.trabajoRepo = trabajoRepo;
    }

    public List<AlbaranCliente> findAll() {
        return albaranRepo.findAll();
    }

    public List<AlbaranCliente> findByEmpresa(String empresa) {
        return albaranRepo.findByEmpresaOrderByFechaEmisionDescIdDesc(empresa);
    }

    public List<AlbaranCliente> findByCliente(Long clienteId) {
        return albaranRepo.findByClienteIdOrderByFechaEmisionDescIdDesc(clienteId);
    }

    public AlbaranCliente findById(Long id) {
        return albaranRepo.findById(id).orElse(null);
    }

    @Transactional
    public AlbaranCliente crearDesdeCliente(Long clienteId) {

        Cliente c = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        AlbaranCliente a = new AlbaranCliente();
        a.setCliente(c);

        // ✅ IMPORTANTE: empresa = TENANT del cliente (NO Argasa/Luga)
        a.setEmpresa(c.getEmpresa());

        // ===== SNAPSHOT CLIENTE =====
        a.setNombreApellidos(c.getNombreApellidos());
        a.setDireccion(c.getDireccion());
        a.setCodigoPostal(c.getCodigoPostal());
        a.setPoblacion(c.getPoblacion());
        a.setProvincia(c.getProvincia());
        a.setTelefono(c.getTelefono());
        a.setMovil(c.getMovil());
        a.setCifDni(c.getCifDni());
        a.setEmail(c.getEmail());
        a.setEmpresa(c.getEmpresa()); // esto NO puede ser null


        // ===== COPIAR TRABAJOS -> LÍNEAS =====
        List<Trabajo> trabajos = trabajoRepo.findByClienteId(clienteId);

        if (trabajos != null) {
            for (Trabajo t : trabajos) {
                if (t == null) continue;

                String desc = (t.getDescripcion() != null) ? t.getDescripcion().trim() : "";
                if (desc.isBlank()) continue;

                double importe = safe(t.getImporte());
                if (importe <= 0) continue;

                LineaAlbaranCliente l = new LineaAlbaranCliente();
                l.setEmpresa(a.getEmpresa());          // ✅ IMPORTANTE (evita el 500)
                l.setCodigo(null);
                l.setDescripcion(desc);
                l.setUnidades(1.0);
                l.setPrecio(importe);
                l.setDtoPct(0.0);

                l.setAlbaran(a);
                l.recalcular();

                a.getLineas().add(l);

        }
        }

        a.recalcularTotales();
        return albaranRepo.save(a);
    }



    private double safe(Double v) {
        return v != null ? v : 0.0;
    }

    @Transactional
    public AlbaranCliente save(AlbaranCliente albaran) {
    	if (albaran.getLineas() != null) {
    		  for (LineaAlbaranCliente l : albaran.getLineas()) {
    		    if (l == null) continue;

    		    l.setAlbaran(albaran);

    		    if (l.getEmpresa() == null || l.getEmpresa().isBlank()) {
    		      l.setEmpresa(albaran.getEmpresa());   // ✅
    		    }

    		    l.recalcular();
    		  }
    		}

        albaran.recalcularTotales();
        return albaranRepo.save(albaran);
    }

    @Transactional
    public void deleteById(Long id) {
        albaranRepo.deleteById(id);
    }

    @Transactional
    public AlbaranCliente agregarLinea(Long albaranId, LineaAlbaranCliente linea) {
        AlbaranCliente a = findById(albaranId);
        if (a == null) throw new RuntimeException("Albarán no encontrado");
        if (linea == null) throw new RuntimeException("Línea inválida");

        linea.setAlbaran(a);

        if (linea.getEmpresa() == null || linea.getEmpresa().isBlank()) {
          linea.setEmpresa(a.getEmpresa());   // ✅
        }

        linea.recalcular();

        a.getLineas().add(linea);
        a.recalcularTotales();

        return albaranRepo.save(a);
    }

    @Transactional
    public AlbaranCliente eliminarLinea(Long albaranId, Long lineaId) {
        AlbaranCliente a = findById(albaranId);
        if (a == null) throw new RuntimeException("Albarán no encontrado");

        if (a.getLineas() != null) {
            a.getLineas().removeIf(l -> l != null && l.getId() != null && l.getId().equals(lineaId));
        }

        a.recalcularTotales();
        return albaranRepo.save(a);
    }

    @Transactional
    public AlbaranCliente confirmar(Long albaranId) {
        AlbaranCliente a = findById(albaranId);
        if (a == null) throw new RuntimeException("Albarán no encontrado");

        a.setConfirmado(true);
        a.recalcularTotales();

        return albaranRepo.save(a);
    }
}
