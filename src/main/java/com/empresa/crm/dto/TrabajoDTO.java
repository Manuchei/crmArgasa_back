package com.empresa.crm.dto;

public class TrabajoDTO {

    private Long id;
    private String descripcion;
    private Double importe;
    private Double pagado;

    public TrabajoDTO(Long id, String descripcion, Double importe, Double pagado) {
        this.id = id;
        this.descripcion = descripcion;
        this.importe = importe;
        this.pagado = pagado;
    }

    // getters y setters
}
