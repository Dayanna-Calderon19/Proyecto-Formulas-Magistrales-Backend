package com.example.proyecto_formulas_magistrales.DTO;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoRequestDTO {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "Los nombres solo pueden contener letras y espacios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "Los apellidos solo pueden contener letras y espacios")
    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe contener exactamente 8 dígitos numéricos")
    private String dni;

    @NotBlank(message = "El número de colegiatura es obligatorio")
    @Size(min = 3, max = 30, message = "El número de colegiatura debe tener entre 3 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "La colegiatura solo puede contener letras, números y guiones")
    private String colegiatura;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(min = 2, max = 100, message = "La especialidad debe tener entre 2 y 100 caracteres")
    private String especialidad;

    @Pattern(regexp = "^[0-9+()\\s-]{7,20}$", message = "El número telefónico debe tener un formato válido (entre 7 y 20 caracteres)")
    private String telefono;

    @Email(message = "Debe proporcionar un formato de correo electrónico válido")
    @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres")
    private String correo;

    @Builder.Default
    private Boolean estado = true;

    // Identificador opcional del usuario con rol MEDICO a vincular
    private Long idUsuario;
}
