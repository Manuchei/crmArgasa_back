package com.empresa.crm.dto;

import lombok.Data;

@Data
public class EventoCalendarioDTO {
    private Long id;
    private String title;
    private String start; // FullCalendar
    private String estado;
    private String observaciones;

    // opcional si quieres mantenerlo (pero ya no hace falta)
    private String fecha;
}
