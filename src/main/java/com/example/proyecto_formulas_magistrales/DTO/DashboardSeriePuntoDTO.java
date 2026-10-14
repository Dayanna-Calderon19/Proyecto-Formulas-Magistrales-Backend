package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSeriePuntoDTO {
    private String etiqueta;
    private LocalDate fecha;
    private long citas;
    private long consultas;
    private long formulas;
}
