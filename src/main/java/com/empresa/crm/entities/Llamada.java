package com.empresa.crm.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "llamadas")
public class Llamada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "empresa", nullable = false, length = 20)
    private String empresa;

    private String motivo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    private String estado;

    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
