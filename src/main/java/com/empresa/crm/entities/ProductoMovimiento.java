package com.empresa.crm.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "productos_movimientos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductoMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private String tipo; // ENTRADA / SALIDA

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "unidades_anteriores", nullable = false)
    private Integer unidadesAnteriores;

    @Column(name = "unidades_nuevas", nullable = false)
    private Integer unidadesNuevas;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
