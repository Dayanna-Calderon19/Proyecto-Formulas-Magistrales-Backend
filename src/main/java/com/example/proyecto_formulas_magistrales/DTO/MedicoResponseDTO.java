package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoResponseDTO {
    private Long idMedico;
    private String nombres;
    private String apellidos;
    private String dni;
    private String colegiatura;
    private String especialidad;
    private String telefono;
    private String correo;
    private Boolean estado;

    // Vinculación de usuario (sin contraseñas ni secretos)
    private Long idUsuario;
    private String username;
    private String nombreCompletoUsuario;
    private Boolean usuarioActivo;

    private Integer totalConsultas;
}
