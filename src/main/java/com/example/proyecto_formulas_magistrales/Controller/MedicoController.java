package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaHistorialDTO;
import com.example.proyecto_formulas_magistrales.DTO.MedicoRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.MedicoResponseDTO;
import com.example.proyecto_formulas_magistrales.DTO.UsuarioResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.MedicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoService medicoService;

    // Listar médicos con filtros (ADMIN y MEDICO)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<MedicoResponseDTO>> listarMedicos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) Boolean estado) {
        return ResponseEntity.ok(medicoService.listarTodos(search, especialidad, estado));
    }

    // Obtener médico por ID (ADMIN y MEDICO)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<MedicoResponseDTO> obtenerMedico(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.obtenerMedico(id));
    }

    // Buscar médico por DNI (ADMIN y MEDICO)
    @GetMapping("/dni/{dni}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<MedicoResponseDTO> buscarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(medicoService.buscarPorDni(dni));
    }

    // Listar especialidades registradas (ADMIN y MEDICO)
    @GetMapping("/especialidades")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<String>> listarEspecialidades() {
        return ResponseEntity.ok(medicoService.listarEspecialidades());
    }

    // Listar usuarios con rol MEDICO disponibles para vinculación (ADMIN)
    @GetMapping("/usuarios-disponibles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuariosDisponibles() {
        return ResponseEntity.ok(medicoService.listarUsuariosMedicosDisponibles());
    }

    // Obtener historial de consultas de un médico (ADMIN y MEDICO)
    @GetMapping("/{id}/consultas")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<List<ConsultaHistorialDTO>> obtenerConsultasMedico(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.obtenerConsultasMedico(id));
    }

    // Registrar un nuevo médico (solo ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicoResponseDTO> registrarMedico(@Valid @RequestBody MedicoRequestDTO dto) {
        MedicoResponseDTO creado = medicoService.registrarMedico(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Actualizar datos del médico (solo ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicoResponseDTO> actualizarMedico(
            @PathVariable Long id,
            @Valid @RequestBody MedicoRequestDTO dto) {
        MedicoResponseDTO actualizado = medicoService.actualizarMedico(id, dto);
        return ResponseEntity.ok(actualizado);
    }

    // Activar médico (solo ADMIN)
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activarMedico(@PathVariable Long id) {
        medicoService.cambiarEstado(id, true);
        return ResponseEntity.ok(Collections.singletonMap("message", "Médico activado correctamente"));
    }

    // Desactivar médico (solo ADMIN)
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> desactivarMedico(@PathVariable Long id) {
        medicoService.cambiarEstado(id, false);
        return ResponseEntity.ok(Collections.singletonMap("message", "Médico desactivado correctamente"));
    }

    // Eliminar médico físicamente (solo ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> eliminarMedico(@PathVariable Long id) {
        medicoService.eliminarMedico(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Médico eliminado exitosamente del sistema"));
    }

    // Desvincular usuario de la ficha médica (solo ADMIN)
    @DeleteMapping("/{id}/usuario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicoResponseDTO> desvincularUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.desvincularUsuario(id));
    }

    // Estadísticas
    @GetMapping("/stats/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Long>> totalMedicos() {
        return ResponseEntity.ok(Collections.singletonMap("total", medicoService.totalMedicos()));
    }

    @GetMapping("/stats/activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
    public ResponseEntity<Map<String, Long>> totalMedicosActivos() {
        return ResponseEntity.ok(Collections.singletonMap("totalActivos", medicoService.totalMedicosActivos()));
    }
}
