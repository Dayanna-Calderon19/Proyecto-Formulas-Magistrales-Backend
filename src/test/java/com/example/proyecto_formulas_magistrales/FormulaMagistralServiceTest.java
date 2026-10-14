package com.example.proyecto_formulas_magistrales;

import com.example.proyecto_formulas_magistrales.Model.FormulaMagistral;
import com.example.proyecto_formulas_magistrales.Repository.FormulaMagistralRepository;
import com.example.proyecto_formulas_magistrales.Service.FormulaMagistralService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FormulaMagistralServiceTest {
    @Mock
    private FormulaMagistralRepository formulaRepository;
    @InjectMocks
    private FormulaMagistralService formulaService;

    @Test
    void shouldCreateFormulaSuccessfully() {
        FormulaMagistral paracetamol = new FormulaMagistral(null, "Paracetamol", "Analgésico",
                "Paracetamol 500mg", BigDecimal.valueOf(100), "comprimidos", LocalDate.now(), LocalDate.now().plusMonths(6), "ACTIVO", null);

        when(formulaRepository.existsByNombreIgnoreCase("Paracetamol")).thenReturn(false);
        when(formulaRepository.save(paracetamol)).thenReturn(paracetamol);

        FormulaMagistral resultado = formulaService.crearFormula(paracetamol);

        assertNotNull(resultado);
        assertEquals("Paracetamol", resultado.getNombre());
        assertEquals("Paracetamol 500mg", resultado.getComposicion());
        assertEquals(BigDecimal.valueOf(100), resultado.getCantidad());
    }

    @Test
    void shouldThrowExceptionWhenFormulaIsDuplicate(){
        FormulaMagistral paracetamol = new FormulaMagistral(null, "Paracetamol", "Analgésico",
                "Paracetamol 500mg", BigDecimal.valueOf(100), "comprimidos", LocalDate.now(), LocalDate.now().plusMonths(6),"ACTIVO", null);
        when(formulaRepository.existsByNombreIgnoreCase("Paracetamol")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> formulaService.crearFormula(paracetamol));
        verify(formulaRepository, never()).save(any());
    }

    @Test
    void shouldGetFormulaById() {
        FormulaMagistral paracetamol = new FormulaMagistral(2L, "Paracetamol", "Analgésico",
                "Paracetamol 500mg", BigDecimal.valueOf(100), "comprimidos", LocalDate.now(), LocalDate.now().plusMonths(6), "ACTIVO", null);

        when(formulaRepository.findById(2L)).thenReturn(Optional.of(paracetamol));

        FormulaMagistral resultado = formulaService.obtenerFormula(2L);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getId());
        assertEquals("Paracetamol", resultado.getNombre());
    }

    @Test
    void shouldUpdateFormulaSuccessfully() {
        FormulaMagistral paracetamol = new FormulaMagistral(2L, "Paracetamol", "Analgésico",
                "Paracetamol 500mg", BigDecimal.valueOf(100), "comprimidos", LocalDate.now(), LocalDate.now().plusMonths(6), "ACTIVO", null);
        FormulaMagistral actualizada = new FormulaMagistral(2L, "Paracetamol", "Analgésico",
                "Paracetamol 500mg", BigDecimal.valueOf(75), "comprimidos", LocalDate.now(), LocalDate.now().plusMonths(6), "ACTIVO", null);

        when(formulaRepository.findById(2L)).thenReturn(Optional.of(paracetamol));
        when(formulaRepository.save(any())).thenReturn(actualizada);

        FormulaMagistral resultado = formulaService.actualizarFormula(2L, actualizada);

        assertEquals(BigDecimal.valueOf(75), resultado.getCantidad());
    }

    @Test
    void shouldDesactivateFormulaSuccessfully(){
        FormulaMagistral paracetamol = new FormulaMagistral(2L, "Paracetamol", "Analgésico",
                "Paracetamol 500mg", BigDecimal.valueOf(100), "comprimidos", LocalDate.now(), LocalDate.now().plusMonths(6), "ACTIVO", null);

        when(formulaRepository.findById(2L)).thenReturn(Optional.of(paracetamol));

        formulaService.desactivarFormula(2L);

        assertEquals("INACTIVO", paracetamol.getEstado());
        verify(formulaRepository, times(1)).save(paracetamol);
    }
}
