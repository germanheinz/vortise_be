package com.vortise.gestion.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    @Column(length = 1000)
    private String responsablesObra;
    @Column(length = 1000)
    private String categoriasManoObra;
    private Integer cantidadSectores;
    private Double superficieCubierta;
    private Double superficieSemicubierta;
    private Double superficieDescubierta;
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "empresa_id")
    private Empresa empresaRelacionada;
    @jakarta.persistence.ManyToMany
    @jakarta.persistence.JoinTable(name = "proyecto_subcontratistas",
        joinColumns = @jakarta.persistence.JoinColumn(name = "proyecto_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "catalogo_empresa_id"))
    private Set<CatalogoEmpresa> subcontratistas = new HashSet<>();
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

    public LocalDate getFechaInicio() {
        return this.fechaInicio;
    }

    public LocalDate getFechaFin() {
        return this.fechaFin;
    }

    public String getResponsablesObra() {
        return this.responsablesObra;
    }

    public String getCategoriasManoObra() {
        return this.categoriasManoObra;
    }

    public Integer getCantidadSectores() {
        return this.cantidadSectores;
    }

    public Double getSuperficieCubierta() { return superficieCubierta; }
    public Double getSuperficieSemicubierta() { return superficieSemicubierta; }
    public Double getSuperficieDescubierta() { return superficieDescubierta; }

    public String getEmpresa() {
        return this.empresa;
    }

    public Empresa getEmpresaRelacionada() {
        return this.empresaRelacionada;
    }

    public Set<CatalogoEmpresa> getSubcontratistas() { return subcontratistas; }
    public void setSubcontratistas(Set<CatalogoEmpresa> subcontratistas) { this.subcontratistas = subcontratistas; }

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

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setResponsablesObra(String responsablesObra) {
        this.responsablesObra = responsablesObra;
    }

    public void setCategoriasManoObra(String categoriasManoObra) {
        this.categoriasManoObra = categoriasManoObra;
    }

    public void setCantidadSectores(Integer cantidadSectores) {
        this.cantidadSectores = cantidadSectores;
    }

    public void setSuperficieCubierta(Double superficieCubierta) { this.superficieCubierta = superficieCubierta; }
    public void setSuperficieSemicubierta(Double superficieSemicubierta) { this.superficieSemicubierta = superficieSemicubierta; }
    public void setSuperficieDescubierta(Double superficieDescubierta) { this.superficieDescubierta = superficieDescubierta; }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public void setEmpresaRelacionada(Empresa empresaRelacionada) {
        this.empresaRelacionada = empresaRelacionada;
        if (empresaRelacionada != null) {
            this.empresa = empresaRelacionada.getNombre();
        }
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
        if (this == o) return true;
        if (o == null || EntityIdentity.type(this) != EntityIdentity.type(o)) return false;
        Proyecto other = (Proyecto) o;
        return getId() != null && getId().equals(other.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof Proyecto;
    }

    public int hashCode() { return EntityIdentity.type(this).hashCode(); }

    public String toString() { return "Proyecto(id=" + getId() + ")"; }

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

        public String toString() { return "ProyectoBuilder"; }
    }
}
