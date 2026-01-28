package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.RutaDiaItemDTO;
import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.scheduler.RutaScheduler;
import com.empresa.crm.services.RutaService;
import com.empresa.crm.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final RutaScheduler rutaScheduler;

    @Override
    public List<Ruta> findAll() {
        return rutaRepository.findByEmpresa(TenantContext.get());
    }


    @Override
    public Ruta findById(Long id) {
        return rutaRepository.findById(id).orElse(null);
    }

    @Override
    public Ruta save(Ruta ruta) {

        if (ruta.getEmpresa() == null || ruta.getEmpresa().isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
        }

        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        Ruta guardada = rutaRepository.save(ruta);

        boolean rutaEsHoy = ruta.getFecha() != null && ruta.getFecha().isEqual(hoy);
        boolean despuesDeLas8 = ahora.isAfter(LocalTime.of(8, 0));

        if (rutaEsHoy && despuesDeLas8) {
            rutaScheduler.enviarActualizacionHoy(ruta.getNombreTransportista());
        }

        return guardada;
    }

    @Override
    public void deleteById(Long id) {
        rutaRepository.deleteById(id);
    }

    @Override
    public List<Ruta> findByEstado(String estado) {
        return rutaRepository.findByEmpresaAndEstado(TenantContext.get(), estado);
    }

    @Override
    public List<Ruta> findByNombreTransportista(String nombre) {
        return rutaRepository.findByEmpresaAndNombreTransportistaContainingIgnoreCase(
            TenantContext.get(), nombre
        );
    }

    @Override
    public List<Ruta> findByFecha(LocalDate fecha) {
        return rutaRepository.findByEmpresaAndFecha(TenantContext.get(), fecha);
    }



    @Override
    public Ruta cerrarRuta(Long id) {
        Ruta ruta = findById(id);

        if (ruta != null && !"cerrada".equalsIgnoreCase(ruta.getEstado())) {

            ruta.setEstado("cerrada");
            rutaRepository.save(ruta);

            LocalDate hoy = LocalDate.now();
            LocalTime ahora = LocalTime.now();

            if (ruta.getFecha() != null && ruta.getFecha().isEqual(hoy) && ahora.isAfter(LocalTime.of(8, 0))) {
                rutaScheduler.enviarActualizacionHoy(ruta.getNombreTransportista());
            }
        }

        return ruta;
    }

    @Override
    @Transactional
    public List<Ruta> crearRutasDeUnDia(RutaDiaRequestDTO request) {

        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;          // yyyy-MM-dd
        DateTimeFormatter es  = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // dd/MM/yyyy

        String f = request.getFecha();
        LocalDate fecha;

        try {
            fecha = LocalDate.parse(f, iso);
        } catch (Exception e) {
            fecha = LocalDate.parse(f, es);
        }

        String estadoBase = (request.getEstado() == null || request.getEstado().isBlank())
                ? "pendiente"
                : request.getEstado();

        if (request.getRutas() == null || request.getRutas().isEmpty()) {
            return new ArrayList<>();
        }

        // ✅ Empresa base (request)
        String empresaBase = (request.getEmpresa() == null) ? null : request.getEmpresa().trim();

        List<Ruta> nuevas = new ArrayList<>();

        for (RutaDiaItemDTO item : request.getRutas()) {

            // ✅ Empresa por item (si viene) o por request (si no)
            String empresaFinal = (item.getEmpresa() != null && !item.getEmpresa().isBlank())
                    ? item.getEmpresa().trim()
                    : empresaBase;

            if (empresaFinal == null || empresaFinal.isBlank()) {
                throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA) en request.empresa o en cada item.empresa");
            }

            Ruta r = new Ruta();
            r.setFecha(fecha);
            r.setNombreTransportista(request.getNombreTransportista());
            r.setEmailTransportista(request.getEmailTransportista());

            r.setOrigen(item.getOrigen());
            r.setDestino(item.getDestino());
            r.setTarea(item.getTarea());
            r.setObservaciones(item.getObservaciones());

            String estadoFinal = (item.getEstado() == null || item.getEstado().isBlank())
                    ? estadoBase
                    : item.getEstado();

            r.setEstado(estadoFinal);

            // ✅ AQUÍ ESTABA EL FALLO
            r.setEmpresa(empresaFinal);

            nuevas.add(r);
        }

        List<Ruta> guardadas = rutaRepository.saveAll(nuevas);

        // Mantener tu comportamiento de "si hoy y >08:00 mandar actualización"
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        if (fecha.isEqual(hoy) && ahora.isAfter(LocalTime.of(8, 0))) {
            rutaScheduler.enviarActualizacionHoy(request.getNombreTransportista());
        }

        return guardadas;
    }

}
