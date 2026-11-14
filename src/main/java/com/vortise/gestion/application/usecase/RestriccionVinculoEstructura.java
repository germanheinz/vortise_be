package com.vortise.gestion.application.usecase;

import com.vortise.gestion.application.exception.SolicitudInvalidaException;
import com.vortise.gestion.domain.model.RestriccionObra;
import com.vortise.gestion.domain.model.RubroObra;
import com.vortise.gestion.domain.repository.RubroObraRepository;
import com.vortise.gestion.domain.service.EstructuraRubros;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Links a restriction to any node of the project structure: a rubro, a nivel or a subrubro (activity). */
final class RestriccionVinculoEstructura {
    private RestriccionVinculoEstructura() { }

    static RubroObra resolver(Long rubroId, Long proyectoId, RubroObraRepository rubroRepository) {
        if (rubroId == null) return null;
        return rubroRepository.findById(rubroId)
            .filter(candidate -> candidate.getProyecto() != null && candidate.getProyecto().getId().equals(proyectoId))
            .orElseThrow(() -> new SolicitudInvalidaException("El rubro, nivel o subrubro debe pertenecer a la misma obra"));
    }

    static void aplicar(RestriccionObra restriccion, RubroObra nodo) {
        restriccion.setRubroVinculado(nodo);
        restriccion.setNivel(nodo.getNivel());
        restriccion.setUnidad(nodo.getUnidad());
        restriccion.setRubro(etiqueta(nodo));
    }

    /** "Rubro · Subrubro" for an activity; the "Rubro › Nivel" path for a grouping node. */
    private static String etiqueta(RubroObra nodo) {
        boolean actividad = nodo.getSubRubro() != null && !nodo.getSubRubro().isBlank()
            && EstructuraRubros.esHoja(nodo, nodo.getProyecto().getRubros());
        if (actividad) {
            return Stream.of(nodo.getRubro(), nodo.getSubRubro())
                .filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" · "));
        }
        List<String> ruta = new ArrayList<>();
        for (RubroObra actual = nodo; actual != null && ruta.size() < EstructuraRubros.PROFUNDIDAD_MAXIMA; actual = actual.getParent()) {
            String nombre = actual.getNombre() != null && !actual.getNombre().isBlank() ? actual.getNombre() : actual.getRubro();
            if (nombre != null && !nombre.isBlank()) ruta.add(nombre);
        }
        Collections.reverse(ruta);
        return String.join(" › ", ruta);
    }
}
