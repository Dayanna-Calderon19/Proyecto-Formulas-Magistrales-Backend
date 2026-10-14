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
public class ConsultaResumenDTO {
    private Long idConsulta;
    private LocalDateTime fechaConsulta;
    private String motivo;
    private String diagnostico;

    // Paciente
    private Long idPaciente;
    private String nombrePaciente;
    private String dniPaciente;

    // Médico
    private Long idMedico;
    private String nombreMedico;
    private String especialidadMedico;
}
