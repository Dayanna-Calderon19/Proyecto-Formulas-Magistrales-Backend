package com.example.proyecto_formulas_magistrales.Controller;

import com.example.proyecto_formulas_magistrales.DTO.ConsultaResumenDTO;
import com.example.proyecto_formulas_magistrales.DTO.FormulaMagistralRequestDTO;
import com.example.proyecto_formulas_magistrales.DTO.FormulaMagistralResponseDTO;
import com.example.proyecto_formulas_magistrales.Service.FormulaMagistralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/formulas-magistrales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MEDICO')")
public class FormulaMagistralController {

    private final FormulaMagistralService formulaService;

    // Listar fórmulas magistrales con filtros de búsqueda y estado
    @GetMapping
    public ResponseEntity<List<FormulaMagistralResponseDTO>> listarTodas(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = esAdmin(authentication);
        return ResponseEntity.ok(formulaService.listarTodas(search, estado, username, isAdmin));
    }

    // Listar fórmulas activas o disponibles
    @GetMapping("/activas")
    public ResponseEntity<List<FormulaMagistralResponseDTO>> listarActivas(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = esAdmin(authentication);
        return ResponseEntity.ok(formulaService.listarTodas(null, "ACTIVO", username, isAdmin));
    }

    // Obtener fórmula por ID
    @GetMapping("/{id}")
    public ResponseEntity<FormulaMagistralResponseDTO> obtenerFormula(@PathVariable Long id) {
        return ResponseEntity.ok(formulaService.obtenerFormulaDTO(id));
    }

    // Listar consultas médicas disponibles para asociar
    @GetMapping("/consultas-disponibles")
    public ResponseEntity<List<ConsultaResumenDTO>> listarConsultasDisponibles(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = esAdmin(authentication);
        return ResponseEntity.ok(formulaService.listarConsultasParaAsociar(username, isAdmin));
    }

    // Crear una nueva fórmula magistral
    @PostMapping
    public ResponseEntity<FormulaMagistralResponseDTO> crearFormula(
            @Valid @RequestBody FormulaMagistralRequestDTO dto,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = esAdmin(authentication);
        FormulaMagistralResponseDTO creada = formulaService.crearFormula(dto, username, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Actualizar datos de una fórmula magistral
    @PutMapping("/{id}")
    public ResponseEntity<FormulaMagistralResponseDTO> actualizarFormula(
            @PathVariable Long id,
            @Valid @RequestBody FormulaMagistralRequestDTO dto,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = esAdmin(authentication);
        FormulaMagistralResponseDTO actualizada = formulaService.actualizarFormula(id, dto, username, isAdmin);
        return ResponseEntity.ok(actualizada);
    }

    // Cambiar estado de la fórmula
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Map<String, String>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado) {
        formulaService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(Collections.singletonMap("message", "Estado de la fórmula actualizado a " + nuevoEstado));
    }

    // Desactivar o anular fórmula magistral (baja lógica)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> desactivarFormula(@PathVariable Long id) {
        formulaService.desactivarFormula(id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Fórmula magistral anulada exitosamente"));
    }

    // Estadísticas
    @GetMapping("/stats/total")
    public ResponseEntity<Map<String, Long>> totalFormulas() {
        return ResponseEntity.ok(Collections.singletonMap("total", formulaService.totalFormulas()));
    }

    @GetMapping("/stats/activas")
    public ResponseEntity<Map<String, Long>> formulasActivas() {
        return ResponseEntity.ok(Collections.singletonMap("totalActivas", formulaService.totalFormulasActivas()));
    }

    private boolean esAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_ADMIN") || a.equalsIgnoreCase("ADMIN"));
    }
}
