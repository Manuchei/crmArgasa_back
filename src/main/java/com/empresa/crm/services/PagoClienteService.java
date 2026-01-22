package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.PagoClienteCreateRequest;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.PagoCliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.PagoClienteRepository;

@Service
public class PagoClienteService {

    private final PagoClienteRepository pagoRepo;
    private final ClienteRepository clienteRepo;

    public PagoClienteService(PagoClienteRepository pagoRepo,
                              ClienteRepository clienteRepo) {
        this.pagoRepo = pagoRepo;
        this.clienteRepo = clienteRepo;
    }

    public List<PagoCliente> listarPorCliente(Long clienteId) {
        return pagoRepo.findByClienteIdOrderByFechaAscIdAsc(clienteId);
    }

    @Transactional
    public PagoCliente crearPago(Long clienteId, PagoClienteCreateRequest req) {
        Cliente c = clienteRepo.findById(clienteId).orElse(null);
        if (c == null) throw new RuntimeException("Cliente no encontrado");

        double importe = safe(req.getImporte());
        if (importe <= 0) throw new RuntimeException("Importe inválido");

        PagoCliente p = new PagoCliente();
        p.setCliente(c);
        p.setFecha(req.getFecha() != null ? req.getFecha() : LocalDate.now());
        p.setImporte(importe);
        p.setMetodo(req.getMetodo());
        p.setObservaciones(req.getObservaciones());

        // ✅ SOLO guardo el pago en historial
        // ❌ NO recalculo ni reparto pagos en trabajos
        return pagoRepo.save(p);
    }

    @Transactional
    public void eliminarPago(Long pagoId) {
        PagoCliente p = pagoRepo.findById(pagoId).orElse(null);
        if (p == null) throw new RuntimeException("Pago no encontrado");

        // ✅ SOLO borro el pago del historial
        // ❌ NO recalculo ni reparto pagos en trabajos
        pagoRepo.deleteById(pagoId);
    }

    private double safe(Double v) {
        return v != null ? v : 0.0;
    }

    // -----------------------------------------------------------------
    // ✅ VERSIÓN ANTIGUA (NO BORRAR) - Reparte pagos entre trabajos
    // La dejo aquí comentada tal y como pediste.
    // -----------------------------------------------------------------

//    /**
//     * ✅ Reparte el total pagado (suma de pagos) entre los trabajos del cliente.
//     * Estrategia: reinicia importePagado de trabajos y vuelve a aplicar pagos en orden.
//     */
//    @Transactional
//    public void recalcularPagosCliente(Long clienteId) {
//        List<Trabajo> trabajos = trabajoRepo.findByClienteIdOrderByIdAsc(clienteId);
//        List<PagoCliente> pagos = pagoRepo.findByClienteIdOrderByFechaAscIdAsc(clienteId);
//
//        // Reset trabajos
//        if (trabajos != null) {
//            for (Trabajo t : trabajos) {
//                if (t == null) continue;
//                t.setImportePagado(0.0);
//                t.setPagado(false);
//            }
//        }
//
//        double saldoPagos = 0.0;
//        if (pagos != null) {
//            for (PagoCliente p : pagos) {
//                saldoPagos += safe(p.getImporte());
//            }
//        }
//
//        if (trabajos == null || trabajos.isEmpty() || saldoPagos <= 0) {
//            if (trabajos != null) trabajoRepo.saveAll(trabajos);
//            return;
//        }
//
//        // Reparto
//        for (Trabajo t : trabajos) {
//            if (t == null) continue;
//
//            double importeTrabajo = safe(t.getImporte());
//            if (importeTrabajo <= 0) continue;
//
//            double pagadoActual = safe(t.getImportePagado());
//            double pendiente = importeTrabajo - pagadoActual;
//            if (pendiente <= 0) {
//                t.setPagado(true);
//                continue;
//            }
//
//            if (saldoPagos <= 0) break;
//
//            double aplica = Math.min(saldoPagos, pendiente);
//            t.setImportePagado(pagadoActual + aplica);
//            saldoPagos -= aplica;
//
//            double pendienteFinal = importeTrabajo - safe(t.getImportePagado());
//            t.setPagado(pendienteFinal <= 0.000001);
//        }
//
//        trabajoRepo.saveAll(trabajos);
//    }
}
