package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardProximaCitaDTO {
    private Long idCita;
    private LocalDateTime fechaHora;
    private Long idPaciente;
    private String nombrePaciente;
    private String dniPaciente;
    private Long idMedico;
    private String nombreMedico;
    private String especialidadMedico;
    private String motivo;
    private String estado;
}
