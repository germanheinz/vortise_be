package com.vortise.gestion.domain.model;

import java.util.List;

/**
 * Structure variant chosen per root rubro. The list holds the node kind of each layer, starting at layer 1 (the rubro).
 * The last layer is always the leaf, which is the only node that carries activity data.
 */
public enum VarianteEstructura {
    RUBRO_SUBRUBRO(List.of(TipoNodoRubro.RUBRO, TipoNodoRubro.SUBRUBRO)),
    RUBRO_NIVEL_SUBRUBRO(List.of(TipoNodoRubro.RUBRO, TipoNodoRubro.NIVEL, TipoNodoRubro.SUBRUBRO)),
    RUBRO_SUBRUBRO_NIVEL(List.of(TipoNodoRubro.RUBRO, TipoNodoRubro.SUBRUBRO, TipoNodoRubro.NIVEL));

    private final List<TipoNodoRubro> capas;

    VarianteEstructura(List<TipoNodoRubro> capas) {
        this.capas = capas;
    }

    /** Number of layers, including the root rubro. */
    public int profundidad() {
        return capas.size();
    }

    /** Node kind expected at the given 1-based layer. */
    public TipoNodoRubro tipoEnCapa(int capa) {
        if (capa < 1 || capa > capas.size()) {
            throw new IllegalArgumentException("Capa fuera de rango para la variante " + name());
        }
        return capas.get(capa - 1);
    }

    /** Whether the variant has a layer of the given kind. */
    public boolean tiene(TipoNodoRubro tipo) {
        return capas.contains(tipo);
    }
}
