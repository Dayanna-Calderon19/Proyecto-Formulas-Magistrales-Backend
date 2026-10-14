package com.example.proyecto_formulas_magistrales.Service;

import com.example.proyecto_formulas_magistrales.DTO.RolCreateDTO;
import com.example.proyecto_formulas_magistrales.DTO.RolResponseDTO;
import com.example.proyecto_formulas_magistrales.Model.Rol;
import com.example.proyecto_formulas_magistrales.Repository.RolRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    @Transactional
    public RolResponseDTO registrarRol(RolCreateDTO dto) {
        String cleanNombre = dto.getNombre().trim().toUpperCase().replace("ROLE_", "");
        if (rolRepository.existsByNombre(cleanNombre)) {
            throw new IllegalStateException("El rol '" + cleanNombre + "' ya existe en el sistema");
        }
        Rol nuevo = new Rol();
        nuevo.setNombre(cleanNombre);
        Rol guardado = rolRepository.save(nuevo);
        return mapToDTO(guardado);
    }

    @Transactional(readOnly = true)
    public RolResponseDTO obtenerRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol con ID " + id + " no encontrado"));
        return mapToDTO(rol);
    }

    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RolResponseDTO buscarPorNombre(String nombre) {
        String clean = nombre.trim().toUpperCase().replace("ROLE_", "");
        Rol rol = rolRepository.findByNombre(clean)
                .orElseThrow(() -> new EntityNotFoundException("Rol '" + clean + "' no encontrado"));
        return mapToDTO(rol);
    }

    @Transactional(readOnly = true)
    public boolean existeRol(String nombre) {
        String clean = nombre.trim().toUpperCase().replace("ROLE_", "");
        return rolRepository.existsByNombre(clean);
    }

    @Transactional(readOnly = true)
    public long totalRoles() {
        return rolRepository.count();
    }

    public RolResponseDTO mapToDTO(Rol rol) {
        return RolResponseDTO.builder()
                .idRol(rol.getIdRol())
                .nombre(rol.getNombre())
                .build();
    }
}
