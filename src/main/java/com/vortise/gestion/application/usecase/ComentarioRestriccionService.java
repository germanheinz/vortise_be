package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.dto.RestriccionComentarioCreateDto;
import com.vortise.gestion.application.dto.RestriccionComentarioDto;
import com.vortise.gestion.application.exception.RecursoNoEncontradoException;
import com.vortise.gestion.domain.model.RestriccionComentario;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.repository.RestriccionComentarioRepository;
import com.vortise.gestion.domain.repository.RestriccionObraRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ComentarioRestriccionService {
    private final RestriccionComentarioRepository comentarioRepository;
    private final RestriccionObraRepository restriccionRepository;

    public ComentarioRestriccionService(RestriccionComentarioRepository comentarioRepository, RestriccionObraRepository restriccionRepository) {
        this.comentarioRepository = comentarioRepository;
        this.restriccionRepository = restriccionRepository;
    }

    public RestriccionComentarioDto crear(Long restriccionId, RestriccionComentarioCreateDto dto) {
        RestriccionObra restriccion = restriccionRepository.findById(restriccionId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Restriccion no encontrada"));

        RestriccionComentario comentario = new RestriccionComentario();
        comentario.setRestriccion(restriccion);
        comentario.setAutor(dto.autor());
        comentario.setMensaje(dto.mensaje());
        comentario.setCreadoEn(LocalDateTime.now());

        return RestriccionMapper.toComentarioDto(comentarioRepository.save(comentario));
    }

    public List<RestriccionComentarioDto> listar(Long restriccionId) {
        return comentarioRepository.findByRestriccionIdOrderByCreadoEnAsc(restriccionId)
            .stream()
            .map(RestriccionMapper::toComentarioDto)
            .toList();
    }
}
