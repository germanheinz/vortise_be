package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.UnidadMetricaEmpresaDto;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.UnidadMetricaEmpresa;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmpresaConfiguracionUseCaseUnidadTest {
    private static final Long EMPRESA_ID = 7L;
    private Empresa empresa;
    private UnidadMetricaEmpresaRepository unidades;
    private EmpresaConfiguracionUseCase useCase;

    @BeforeEach
    void setUp() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(EMPRESA_ID);
        when(empresas.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        when(empresas.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        unidades = mock(UnidadMetricaEmpresaRepository.class);
        useCase = new EmpresaConfiguracionUseCase(
            empresas,
            mock(ResponsableEmpresaRepository.class),
            unidades,
            mock(CatalogoEmpresaRepository.class),
            mock(CalendarioLaboralEmpresaRepository.class),
            mock(CategoriaManoObraEmpresaRepository.class)
        );
        when(unidades.save(any(UnidadMetricaEmpresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void derivesCodeFromNameWhenNoCodeWasRequested() {
        when(unidades.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        var result = useCase.saveUnidad(EMPRESA_ID, new UnidadMetricaEmpresaDto(null, null, " Metro cúbico "));

        assertEquals("METRO_CUBICO", result.codigo());
        assertEquals("Metro cúbico", result.nombre());
    }

    @Test
    void addsDeterministicSuffixWhenGeneratedCodeCollides() {
        when(unidades.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(
            new UnidadMetricaEmpresa(empresa, "METRO_CUBICO", "Metro cúbico existente")
        ));

        var result = useCase.saveUnidad(EMPRESA_ID, new UnidadMetricaEmpresaDto(null, null, "Metro cúbico"));

        assertEquals("METRO_CUBICO_2", result.codigo());
    }

    @Test
    void normalizesAndPreservesExplicitCode() {
        when(unidades.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        var result = useCase.saveUnidad(EMPRESA_ID, new UnidadMetricaEmpresaDto(null, " cm ", "Centímetro"));

        assertEquals("CM", result.codigo());
    }

    @Test
    void keepsExistingCodeWhenUpdatingWithoutAnExplicitReplacement() {
        UnidadMetricaEmpresa existing = new UnidadMetricaEmpresa(empresa, "LEGACY_M", "Metro legado");
        when(unidades.findById(45L)).thenReturn(Optional.of(existing));

        var result = useCase.saveUnidad(EMPRESA_ID, new UnidadMetricaEmpresaDto(45L, null, "Metro actualizado"));

        assertEquals("LEGACY_M", result.codigo());
        assertEquals("Metro actualizado", result.nombre());
    }
}