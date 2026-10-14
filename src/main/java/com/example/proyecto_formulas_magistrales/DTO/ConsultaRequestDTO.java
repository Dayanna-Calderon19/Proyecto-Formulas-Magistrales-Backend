package com.example.proyecto_formulas_magistrales.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaRequestDTO {

    // Cita asociada (opcional si es atención directa)
    private Long idCita;

    // Obligatorio si no hay cita previa; si hay cita, se deriva de ella
    private Long idPaciente;

    // Opcional para médico autenticado; si hay cita, se deriva de ella
    private Long idMedico;

    @Builder.Default
    private LocalDateTime fechaConsulta = LocalDateTime.now();

    @NotBlank(message = "El motivo de la consulta es obligatorio")
    private String motivo;

    @NotBlank(message = "El diagnóstico médico es obligatorio")
    private String diagnostico;

    private String tratamiento;

    private String observaciones;
}
