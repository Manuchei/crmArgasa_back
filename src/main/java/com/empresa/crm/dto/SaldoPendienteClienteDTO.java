package com.empresa.crm.dto;

import java.time.LocalDate;

public class SaldoPendienteClienteDTO {

    private Long clienteId;
    private String nombreApellidos;
    private String telefono;
    private Double saldoPendiente;
    private LocalDate fechaUltimoPago;

    public SaldoPendienteClienteDTO(
            Long clienteId,
            String nombreApellidos,
            String telefono,
            Double saldoPendiente,
            LocalDate fechaUltimoPago) {

        this.clienteId = clienteId;
        this.nombreApellidos = nombreApellidos;
        this.telefono = telefono;
        this.saldoPendiente = saldoPendiente;
        this.fechaUltimoPago = fechaUltimoPago;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getNombreApellidos() {
        return nombreApellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public Double getSaldoPendiente() {
        return saldoPendiente;
    }

    public LocalDate getFechaUltimoPago() {
        return fechaUltimoPago;
    }
}