package com.example.proyecto_formulas_magistrales.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long idUsuario;
    private String username;
    private String nombreCompleto;
    private String correo;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private Set<String> roles;
    private Long idMedico;
    private String nombreMedico;
}
