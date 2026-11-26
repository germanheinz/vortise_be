package com.vortise.gestion.domain.rule.indicadores;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rounding rules shared by every indicator level. */
public final class Redondeo {
    private Redondeo() { }

    /** Whole number rounding half away from zero: 0,5 → 1 and −0,5 → −1. */
    public static int alejandoDelCero(double valor) {
        return BigDecimal.valueOf(valor).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /** Smallest whole number of days that covers the value, ignoring floating point noise (6,0000000001 → 6). */
    public static int haciaArriba(double valor) {
        return BigDecimal.valueOf(valor).setScale(9, RoundingMode.HALF_UP).setScale(0, RoundingMode.CEILING).intValueExact();
    }

    /** Value shown with two decimals; states are classified on it so the label always matches the visible number. */
    static double dosDecimales(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
