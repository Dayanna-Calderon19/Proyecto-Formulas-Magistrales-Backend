package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiDTO {
    private String id;
    private String titulo;
    private long valor;
    private String subtitulo;
    private String icono;
    private String ruta;
    private String variacion;
    private String color;
}
