/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.OneToMany
 *  jakarta.persistence.Table
 */
package com.vortise.gestion.domain.model;

import com.vortise.gestion.domain.model.CostoIndirecto;
import com.vortise.gestion.domain.model.Entrega;
import com.vortise.gestion.domain.model.Ingreso;
import com.vortise.gestion.domain.model.Planta;
import com.vortise.gestion.domain.model.RegistroHoras;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="proyectos")
public class Proyecto {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String nombre;
    private String descripcion;
    private Double presupuestoUsd;
    private String direccion;
    private String numeroProyecto;
    private String empresa;
    private Double horasPrevistas;
    private Double horasReales;
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<Planta> plantas;
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<RubroObra> rubros = new ArrayList<RubroObra>();
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<RestriccionObra> restricciones = new ArrayList<RestriccionObra>();
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<ObraTarea> obraTareas = new ArrayList<ObraTarea>();
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<RegistroHoras> registrosHoras = new ArrayList<RegistroHoras>();
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<Entrega> entregas;
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<CostoIndirecto> costosIndirectos;
    @OneToMany(mappedBy="proyecto", cascade={CascadeType.ALL}, orphanRemoval=true)
    private List<Ingreso> ingresos;

    private static List<Planta> $default$plantas() {
        return new ArrayList<Planta>();
    }

    private static List<Entrega> $default$entregas() {
        return new ArrayList<Entrega>();
    }

    private static List<CostoIndirecto> $default$costosIndirectos() {
        return new ArrayList<CostoIndirecto>();
    }

    private static List<Ingreso> $default$ingresos() {
        return new ArrayList<Ingreso>();
    }

    public static ProyectoBuilder builder() {
        return new ProyectoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getNombre() {
        return this.nombre;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Double getPresupuestoUsd() {
        return this.presupuestoUsd;
    }

    public String getDireccion() {
        return this.direccion;
    }

    public String getNumeroProyecto() {
        return this.numeroProyecto;
    }

    public String getEmpresa() {
        return this.empresa;
    }

    public Double getHorasPrevistas() {
        return this.horasPrevistas;
    }

    public Double getHorasReales() {
        return this.horasReales;
    }

    public List<Planta> getPlantas() {
        return this.plantas;
    }

    public List<RubroObra> getRubros() {
        return this.rubros;
    }

    public List<RestriccionObra> getRestricciones() {
        return this.restricciones;
    }

    public List<ObraTarea> getObraTareas() {
        return this.obraTareas;
    }

    public List<RegistroHoras> getRegistrosHoras() {
        return this.registrosHoras;
    }

    public List<Entrega> getEntregas() {
        return this.entregas;
    }

    public List<CostoIndirecto> getCostosIndirectos() {
        return this.costosIndirectos;
    }

    public List<Ingreso> getIngresos() {
        return this.ingresos;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPresupuestoUsd(Double presupuestoUsd) {
        this.presupuestoUsd = presupuestoUsd;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setNumeroProyecto(String numeroProyecto) {
        this.numeroProyecto = numeroProyecto;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public void setHorasPrevistas(Double horasPrevistas) {
        this.horasPrevistas = horasPrevistas;
    }

    public void setHorasReales(Double horasReales) {
        this.horasReales = horasReales;
    }

    public void setPlantas(List<Planta> plantas) {
        this.plantas = plantas;
    }

    public void setRubros(List<RubroObra> rubros) {
        this.rubros = rubros;
    }

    public void setRestricciones(List<RestriccionObra> restricciones) {
        this.restricciones = restricciones;
    }

    public void setObraTareas(List<ObraTarea> obraTareas) {
        this.obraTareas = obraTareas;
    }

    public void setRegistrosHoras(List<RegistroHoras> registrosHoras) {
        this.registrosHoras = registrosHoras;
    }

    public void setEntregas(List<Entrega> entregas) {
        this.entregas = entregas;
    }

    public void setCostosIndirectos(List<CostoIndirecto> costosIndirectos) {
        this.costosIndirectos = costosIndirectos;
    }

    public void setIngresos(List<Ingreso> ingresos) {
        this.ingresos = ingresos;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Proyecto)) {
            return false;
        }
        Proyecto other = (Proyecto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Double this$presupuestoUsd = this.getPresupuestoUsd();
        Double other$presupuestoUsd = other.getPresupuestoUsd();
        if (this$presupuestoUsd == null ? other$presupuestoUsd != null : !((Object)this$presupuestoUsd).equals(other$presupuestoUsd)) {
            return false;
        }
        String this$nombre = this.getNombre();
        String other$nombre = other.getNombre();
        if (this$nombre == null ? other$nombre != null : !this$nombre.equals(other$nombre)) {
            return false;
        }
        String this$descripcion = this.getDescripcion();
        String other$descripcion = other.getDescripcion();
        if (this$descripcion == null ? other$descripcion != null : !this$descripcion.equals(other$descripcion)) {
            return false;
        }
        List<Planta> this$plantas = this.getPlantas();
        List<Planta> other$plantas = other.getPlantas();
        if (this$plantas == null ? other$plantas != null : !((Object)this$plantas).equals(other$plantas)) {
            return false;
        }
        List<Entrega> this$entregas = this.getEntregas();
        List<Entrega> other$entregas = other.getEntregas();
        if (this$entregas == null ? other$entregas != null : !((Object)this$entregas).equals(other$entregas)) {
            return false;
        }
        List<CostoIndirecto> this$costosIndirectos = this.getCostosIndirectos();
        List<CostoIndirecto> other$costosIndirectos = other.getCostosIndirectos();
        if (this$costosIndirectos == null ? other$costosIndirectos != null : !((Object)this$costosIndirectos).equals(other$costosIndirectos)) {
            return false;
        }
        List<Ingreso> this$ingresos = this.getIngresos();
        List<Ingreso> other$ingresos = other.getIngresos();
        return !(this$ingresos == null ? other$ingresos != null : !((Object)this$ingresos).equals(other$ingresos));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Proyecto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Double $presupuestoUsd = this.getPresupuestoUsd();
        result = result * 59 + ($presupuestoUsd == null ? 43 : ((Object)$presupuestoUsd).hashCode());
        String $nombre = this.getNombre();
        result = result * 59 + ($nombre == null ? 43 : $nombre.hashCode());
        String $descripcion = this.getDescripcion();
        result = result * 59 + ($descripcion == null ? 43 : $descripcion.hashCode());
        List<Planta> $plantas = this.getPlantas();
        result = result * 59 + ($plantas == null ? 43 : ((Object)$plantas).hashCode());
        List<Entrega> $entregas = this.getEntregas();
        result = result * 59 + ($entregas == null ? 43 : ((Object)$entregas).hashCode());
        List<CostoIndirecto> $costosIndirectos = this.getCostosIndirectos();
        result = result * 59 + ($costosIndirectos == null ? 43 : ((Object)$costosIndirectos).hashCode());
        List<Ingreso> $ingresos = this.getIngresos();
        result = result * 59 + ($ingresos == null ? 43 : ((Object)$ingresos).hashCode());
        return result;
    }

    public String toString() {
        return "Proyecto(id=" + this.getId() + ", nombre=" + this.getNombre() + ", descripcion=" + this.getDescripcion() + ", presupuestoUsd=" + this.getPresupuestoUsd() + ", plantas=" + String.valueOf(this.getPlantas()) + ", entregas=" + String.valueOf(this.getEntregas()) + ", costosIndirectos=" + String.valueOf(this.getCostosIndirectos()) + ", ingresos=" + String.valueOf(this.getIngresos()) + ")";
    }

    public Proyecto() {
        this.plantas = Proyecto.$default$plantas();
        this.entregas = Proyecto.$default$entregas();
        this.costosIndirectos = Proyecto.$default$costosIndirectos();
        this.ingresos = Proyecto.$default$ingresos();
    }

    public Proyecto(Long id, String nombre, String descripcion, Double presupuestoUsd, List<Planta> plantas, List<Entrega> entregas, List<CostoIndirecto> costosIndirectos, List<Ingreso> ingresos) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.presupuestoUsd = presupuestoUsd;
        this.plantas = plantas;
        this.entregas = entregas;
        this.costosIndirectos = costosIndirectos;
        this.ingresos = ingresos;
    }

    public static class ProyectoBuilder {
        private Long id;
        private String nombre;
        private String descripcion;
        private Double presupuestoUsd;
        private boolean plantas$set;
        private List<Planta> plantas$value;
        private boolean entregas$set;
        private List<Entrega> entregas$value;
        private boolean costosIndirectos$set;
        private List<CostoIndirecto> costosIndirectos$value;
        private boolean ingresos$set;
        private List<Ingreso> ingresos$value;

        ProyectoBuilder() {
        }

        public ProyectoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ProyectoBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public ProyectoBuilder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        public ProyectoBuilder presupuestoUsd(Double presupuestoUsd) {
            this.presupuestoUsd = presupuestoUsd;
            return this;
        }

        public ProyectoBuilder plantas(List<Planta> plantas) {
            this.plantas$value = plantas;
            this.plantas$set = true;
            return this;
        }

        public ProyectoBuilder entregas(List<Entrega> entregas) {
            this.entregas$value = entregas;
            this.entregas$set = true;
            return this;
        }

        public ProyectoBuilder costosIndirectos(List<CostoIndirecto> costosIndirectos) {
            this.costosIndirectos$value = costosIndirectos;
            this.costosIndirectos$set = true;
            return this;
        }

        public ProyectoBuilder ingresos(List<Ingreso> ingresos) {
            this.ingresos$value = ingresos;
            this.ingresos$set = true;
            return this;
        }

        public Proyecto build() {
            List<Planta> plantas$value = this.plantas$value;
            if (!this.plantas$set) {
                plantas$value = Proyecto.$default$plantas();
            }
            List<Entrega> entregas$value = this.entregas$value;
            if (!this.entregas$set) {
                entregas$value = Proyecto.$default$entregas();
            }
            List<CostoIndirecto> costosIndirectos$value = this.costosIndirectos$value;
            if (!this.costosIndirectos$set) {
                costosIndirectos$value = Proyecto.$default$costosIndirectos();
            }
            List<Ingreso> ingresos$value = this.ingresos$value;
            if (!this.ingresos$set) {
                ingresos$value = Proyecto.$default$ingresos();
            }
            return new Proyecto(this.id, this.nombre, this.descripcion, this.presupuestoUsd, plantas$value, entregas$value, costosIndirectos$value, ingresos$value);
        }

        public String toString() {
            return "Proyecto.ProyectoBuilder(id=" + this.id + ", nombre=" + this.nombre + ", descripcion=" + this.descripcion + ", presupuestoUsd=" + this.presupuestoUsd + ", plantas$value=" + String.valueOf(this.plantas$value) + ", entregas$value=" + String.valueOf(this.entregas$value) + ", costosIndirectos$value=" + String.valueOf(this.costosIndirectos$value) + ", ingresos$value=" + String.valueOf(this.ingresos$value) + ")";
        }
    }
}
