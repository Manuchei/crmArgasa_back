package com.empresa.crm.dto;

public class ClienteResumenDTO {

    private Long id;
    private String nombreApellidos;
    private String telefono;
    private String movil;
    private String cifDni;
    private String email;

    private Double totalImporte;
    private Double totalPagado;
    private Double saldoPendiente;

    public ClienteResumenDTO(Long id,
                             String nombreApellidos,
                             String telefono,
                             String movil,
                             String cifDni,
                             String email,
                             Double totalImporte,
                             Double totalPagado,
                             Double saldoPendiente) {
        this.id = id;
        this.nombreApellidos = nombreApellidos;
        this.telefono = telefono;
        this.movil = movil;
        this.cifDni = cifDni;
        this.email = email;
        this.totalImporte = totalImporte;
        this.totalPagado = totalPagado;
        this.saldoPendiente = saldoPendiente;
    }

    public Long getId() { return id; }
    public String getNombreApellidos() { return nombreApellidos; }
    public String getTelefono() { return telefono; }
    public String getMovil() { return movil; }
    public String getCifDni() { return cifDni; }
    public String getEmail() { return email; }
    public Double getTotalImporte() { return totalImporte; }
    public Double getTotalPagado() { return totalPagado; }
    public Double getSaldoPendiente() { return saldoPendiente; }
}