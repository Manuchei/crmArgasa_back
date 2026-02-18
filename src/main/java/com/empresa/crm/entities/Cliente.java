package com.empresa.crm.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa se asigna desde backend (READ_ONLY)
    @Column(nullable = false, length = 100)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String empresa;

    private String nombreApellidos;
    private String direccion;
    private String codigoPostal;
    private String poblacion;
    private String provincia;
    private String telefono;
    private String movil;
    private String cifDni;
    private String email;

    private Double totalImporte = 0.0;
    private Double totalPagado = 0.0;

    // ---------------------------
    // Helpers / Getters útiles
    // ---------------------------
    @Transient
    public Double getSaldo() {
        double importe = (totalImporte != null) ? totalImporte : 0.0;
        double pagado = (totalPagado != null) ? totalPagado : 0.0;
        return importe - pagado;
    }

    /**
     * ✅ Dirección completa lista para usar en rutas/albaranes.
     * Ejemplo: "C/ Mayor 12, 36201 Vigo (Pontevedra)"
     */
    @Transient
    public String getDireccionCompleta() {
        String dir = safe(direccion);
        String cp = safe(codigoPostal);
        String pob = safe(poblacion);
        String prov = safe(provincia);

        StringBuilder sb = new StringBuilder();

        if (!dir.isBlank()) sb.append(dir);

        // ", 36201 Vigo"
        String cpPob = joinWithSpace(cp, pob).trim();
        if (!cpPob.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cpPob);
        }

        // " (Pontevedra)"
        if (!prov.isBlank()) {
            sb.append(" (").append(prov).append(")");
        }

        return sb.toString().trim();
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String joinWithSpace(String a, String b) {
        a = safe(a);
        b = safe(b);
        if (a.isBlank()) return b;
        if (b.isBlank()) return a;
        return a + " " + b;
    }

    // ---------------------------
    // Relaciones
    // ---------------------------

    // ✅ Trabajos
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("cliente-trabajos")
    private List<Trabajo> trabajos = new ArrayList<>();

    // ✅ Albaranes
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("cliente-albaranes")
    private List<AlbaranCliente> albaranes = new ArrayList<>();

    // ✅ Pagos
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("cliente-pagos")
    private List<PagoCliente> pagos = new ArrayList<>();

    // ============================
    // Helpers trabajos (tuyos)
    // ============================
    public void addTrabajo(Trabajo trabajo) {
        if (trabajo == null) return;

        trabajos.add(trabajo);
        trabajo.setCliente(this);

        double importe = (trabajo.getImporte() != null) ? trabajo.getImporte() : 0.0;
        double pagado = (trabajo.getImportePagado() != null) ? trabajo.getImportePagado() : 0.0;

        totalImporte = (totalImporte != null ? totalImporte : 0.0) + importe;
        totalPagado = (totalPagado != null ? totalPagado : 0.0) + pagado;
    }

    public void removeTrabajo(Trabajo trabajo) {
        if (trabajo == null) return;

        trabajos.remove(trabajo);

        double importe = (trabajo.getImporte() != null) ? trabajo.getImporte() : 0.0;
        double pagado = (trabajo.getImportePagado() != null) ? trabajo.getImportePagado() : 0.0;

        totalImporte = (totalImporte != null ? totalImporte : 0.0) - importe;
        totalPagado = (totalPagado != null ? totalPagado : 0.0) - pagado;

        trabajo.setCliente(null);
    }
}
