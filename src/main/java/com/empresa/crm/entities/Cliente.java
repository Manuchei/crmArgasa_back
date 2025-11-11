package com.empresa.crm.entities;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Data
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String empresa;
    private String telefono;
    private String email;

    private Double totalImporte = 0.0;
    private Double totalPagado = 0.0;

    @Transient
    public Double getSaldo() {
        double importe = (totalImporte != null) ? totalImporte : 0.0;
        double pagado = (totalPagado != null) ? totalPagado : 0.0;
        return importe - pagado;
    }

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Trabajo> trabajos = new ArrayList<>();

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
