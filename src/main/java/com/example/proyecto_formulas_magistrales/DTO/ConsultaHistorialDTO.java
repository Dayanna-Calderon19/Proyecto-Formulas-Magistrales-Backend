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
public class ConsultaHistorialDTO {
    private Long idConsulta;
    private LocalDateTime fechaConsulta;
    private String motivo;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;

    // Datos del paciente
    private Long idPaciente;
    private String nombrePaciente;
    private String dniPaciente;

    // Datos del médico
    private Long idMedico;
    private String nombreMedico;
    private String especialidadMedico;
    private String colegiaturaMedico;
}
