package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionComentarioDto;
import com.vortise.gestion.domain.repository.RestriccionComentarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetRestriccionComentariosUseCase {
    private final RestriccionComentarioRepository comentarioRepository;

    public GetRestriccionComentariosUseCase(RestriccionComentarioRepository comentarioRepository) {
        this.comentarioRepository = comentarioRepository;
    }

    public List<RestriccionComentarioDto> execute(Long restriccionId) {
        return comentarioRepository.findByRestriccionIdOrderByCreadoEnAsc(restriccionId)
            .stream()
            .map(RestriccionMapper::toComentarioDto)
            .toList();
    }
}