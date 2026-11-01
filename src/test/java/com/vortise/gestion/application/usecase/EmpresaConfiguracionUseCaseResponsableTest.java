package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.ResponsableEmpresaDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.Empresa;
import com.vortise.gestion.domain.model.ResponsableEmpresa;
import com.vortise.gestion.domain.repository.CalendarioLaboralEmpresaRepository;
import com.vortise.gestion.domain.repository.CatalogoEmpresaRepository;
import com.vortise.gestion.domain.repository.CategoriaManoObraEmpresaRepository;
import com.vortise.gestion.domain.repository.EmpresaRepository;
import com.vortise.gestion.domain.repository.ResponsableEmpresaRepository;
import com.vortise.gestion.domain.repository.UnidadMetricaEmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmpresaConfiguracionUseCaseResponsableTest {
    private static final Long EMPRESA_ID = 7L;
    private Empresa empresa;
    private ResponsableEmpresaRepository responsables;
    private EmpresaConfiguracionUseCase useCase;

    @BeforeEach
    void setUp() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(EMPRESA_ID);
        when(empresas.bloquearPorId(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        responsables = mock(ResponsableEmpresaRepository.class);
        useCase = new EmpresaConfiguracionUseCase(
            empresas,
            responsables,
            mock(UnidadMetricaEmpresaRepository.class),
            mock(CatalogoEmpresaRepository.class),
            mock(CalendarioLaboralEmpresaRepository.class),
            mock(CategoriaManoObraEmpresaRepository.class)
        );
        when(responsables.save(any(ResponsableEmpresa.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesAndReturnsNormalizedAreaWhenCreatingResponsible() {
        ResponsableEmpresaDto result = useCase.saveResponsable(EMPRESA_ID,
            new ResponsableEmpresaDto(null, "Jefe", "Ana", null, null, " Compras "));

        assertEquals("Compras", result.area());
    }

    @Test
    void clearsAreaWhenUpdatingResponsibleWithBlankArea() {
        ResponsableEmpresa existing = new ResponsableEmpresa(empresa, "Jefe", "Ana", null, null, "Compras");
        when(responsables.findById(12L)).thenReturn(Optional.of(existing));

        ResponsableEmpresaDto result = useCase.saveResponsable(EMPRESA_ID,
            new ResponsableEmpresaDto(12L, "Jefe", "Ana", null, null, "  "));

        assertNull(result.area());
    }

    @Test
    void rejectsResponsibleFromAnotherCompany() {
        Empresa otraEmpresa = mock(Empresa.class);
        when(otraEmpresa.getId()).thenReturn(8L);
        when(responsables.findById(12L)).thenReturn(Optional.of(new ResponsableEmpresa(otraEmpresa, "Jefe", "Ana", null, null)));

        assertThrows(SolicitudInvalidaException.class, () -> useCase.saveResponsable(EMPRESA_ID,
            new ResponsableEmpresaDto(12L, "Jefe", "Ana", null, null, "Compras")));
    }

    @Test
    void rejectsMissingResponsibleOnUpdate() {
        when(responsables.findById(12L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> useCase.saveResponsable(EMPRESA_ID,
            new ResponsableEmpresaDto(12L, "Jefe", "Ana", null, null, "Compras")));
    }
}