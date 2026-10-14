package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMedicoInfoDTO {
    private Long idMedico;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private String especialidad;
    private String colegiatura;
}
