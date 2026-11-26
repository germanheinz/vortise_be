package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.GastoFijoDto;
import com.vortise.gestion.domain.model.GastoFijo;
import com.vortise.gestion.domain.repository.GastoFijoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GastoFijoService {
    private final GastoFijoRepository gastos;

    public GastoFijoService(GastoFijoRepository gastos) {
        this.gastos = gastos;
    }

    public List<GastoFijoDto> listar() {
        return gastos.findAll().stream().map(GastoFijoService::toDto).toList();
    }

    public GastoFijoDto guardar(GastoFijoDto dto) {
        var gasto = GastoFijo.builder().id(dto.id()).concepto(dto.concepto()).monto(dto.monto())
                .categoria(dto.categoria()).mes(dto.mes()).build();
        return toDto(gastos.save(gasto));
    }

    public void eliminar(Long id) {
        gastos.deleteById(id);
    }

    private static GastoFijoDto toDto(GastoFijo gasto) {
        return new GastoFijoDto(gasto.getId(), gasto.getConcepto(), gasto.getMonto(), gasto.getCategoria(), gasto.getMes());
    }
}
