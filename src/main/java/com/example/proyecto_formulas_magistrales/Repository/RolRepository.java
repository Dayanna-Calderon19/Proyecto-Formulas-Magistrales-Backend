package com.example.proyecto_formulas_magistrales.Repository;

import com.example.proyecto_formulas_magistrales.Model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    // Buscar rol por nombre
    Optional<Rol> findByNombre(String nombre);

    // Verificar si el rol ya existe
    boolean existsByNombre(String nombre);
}
