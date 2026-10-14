package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEstadoCountDTO {
    private String estado;
    private long cantidad;
    private double porcentaje;
    private String color;
}
