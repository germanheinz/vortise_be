/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
package com.vortise.gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="gastos_fijos")
public class GastoFijo {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String concepto;
    @Column(nullable=false)
    private Double monto;
    @Column
    private String categoria;
    private Integer mes;

    public static GastoFijoBuilder builder() {
        return new GastoFijoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getConcepto() {
        return this.concepto;
    }

    public Double getMonto() {
        return this.monto;
    }

    public String getCategoria() {
        return this.categoria;
    }

    public Integer getMes() {
        return this.mes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GastoFijo)) {
            return false;
        }
        GastoFijo other = (GastoFijo)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Double this$monto = this.getMonto();
        Double other$monto = other.getMonto();
        if (this$monto == null ? other$monto != null : !((Object)this$monto).equals(other$monto)) {
            return false;
        }
        Integer this$mes = this.getMes();
        Integer other$mes = other.getMes();
        if (this$mes == null ? other$mes != null : !((Object)this$mes).equals(other$mes)) {
            return false;
        }
        String this$concepto = this.getConcepto();
        String other$concepto = other.getConcepto();
        if (this$concepto == null ? other$concepto != null : !this$concepto.equals(other$concepto)) {
            return false;
        }
        String this$categoria = this.getCategoria();
        String other$categoria = other.getCategoria();
        return !(this$categoria == null ? other$categoria != null : !this$categoria.equals(other$categoria));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GastoFijo;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Double $monto = this.getMonto();
        result = result * 59 + ($monto == null ? 43 : ((Object)$monto).hashCode());
        Integer $mes = this.getMes();
        result = result * 59 + ($mes == null ? 43 : ((Object)$mes).hashCode());
        String $concepto = this.getConcepto();
        result = result * 59 + ($concepto == null ? 43 : $concepto.hashCode());
        String $categoria = this.getCategoria();
        result = result * 59 + ($categoria == null ? 43 : $categoria.hashCode());
        return result;
    }

    public String toString() {
        return "GastoFijo(id=" + this.getId() + ", concepto=" + this.getConcepto() + ", monto=" + this.getMonto() + ", categoria=" + this.getCategoria() + ", mes=" + this.getMes() + ")";
    }

    public GastoFijo() {
    }

    public GastoFijo(Long id, String concepto, Double monto, String categoria, Integer mes) {
        this.id = id;
        this.concepto = concepto;
        this.monto = monto;
        this.categoria = categoria;
        this.mes = mes;
    }

    public static class GastoFijoBuilder {
        private Long id;
        private String concepto;
        private Double monto;
        private String categoria;
        private Integer mes;

        GastoFijoBuilder() {
        }

        public GastoFijoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GastoFijoBuilder concepto(String concepto) {
            this.concepto = concepto;
            return this;
        }

        public GastoFijoBuilder monto(Double monto) {
            this.monto = monto;
            return this;
        }

        public GastoFijoBuilder categoria(String categoria) {
            this.categoria = categoria;
            return this;
        }

        public GastoFijoBuilder mes(Integer mes) {
            this.mes = mes;
            return this;
        }

        public GastoFijo build() {
            return new GastoFijo(this.id, this.concepto, this.monto, this.categoria, this.mes);
        }

        public String toString() {
            return "GastoFijo.GastoFijoBuilder(id=" + this.id + ", concepto=" + this.concepto + ", monto=" + this.monto + ", categoria=" + this.categoria + ", mes=" + this.mes + ")";
        }
    }
}
