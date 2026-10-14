package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaResumenDTO {
    private Long idFormula;
    private String nombre;
    private String composicion;
    private BigDecimal cantidad;
    private String unidad;
    private String estado;
}
