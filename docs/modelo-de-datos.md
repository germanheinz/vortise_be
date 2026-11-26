# Modelo de datos de Gestión de Obras

Inventario del backend a partir de las anotaciones JPA y las migraciones SQL del repositorio. El esquema versionado define **43 tablas**: 33 de entidades, 6 auxiliares y 4 históricas. El inventario describe el código y las migraciones, sin consultar una base desplegada.

## Cómo se conecta la obra

`Empresa` → `Proyecto` (obra) → rubros, contratos, restricciones, tareas de obra, registros de horas y otros datos operativos.

`Proyecto` declara nueve colecciones hijas: plantas, rubros, contratos, restricciones, tareas de obra, registros de horas, entregas, costos indirectos e ingresos. Las líneas de presupuesto, los reportes y los certificados también apuntan al proyecto desde su propia entidad. Los subcontratistas se vinculan al catálogo de empresa mediante una tabla puente.

## Entidades y tablas

La columna **Relación** indica el vínculo principal de la entidad. Los identificadores guardados como valores se distinguen de las asociaciones declaradas en JPA.

### Empresa, configuración y acceso

| Entidad | Tabla | Relación | Qué guarda |
| --- | --- | --- | --- |
| Empresa | `empresas` | Raíz de empresa | Identificación fiscal, nombre, límite de usuarios y calendario laboral integrado. |
| UsuarioEmpresa | `usuarios_empresa` | Empresa principal; empresas y obras asignadas mediante tablas auxiliares | Cuenta, rol e identidad de autenticación. |
| CatalogoEmpresa | `catalogos_empresa` | Empresa; vinculado a obras como catálogo de subcontratistas | Entradas por tipo, código y nombre. |
| CategoriaManoObraEmpresa | `categorias_mano_obra_empresa` | Empresa | Categoría, costo por hora y habilitación. |
| ResponsableEmpresa | `responsables_empresa` | Empresa | Responsables registrados, rol, área y contacto. |
| UnidadMetricaEmpresa | `unidades_metricas_empresa` | Empresa | Código y nombre de la unidad. |
| InvitacionEquipo | `invitaciones_equipo` | ID del titular y conjunto de empresas seleccionadas | Token de invitación, vencimiento y estado de uso. |
| InvitacionUsuario | `invitaciones_usuario` | IDs de empresa, creador y obras asignadas | Invitación dirigida a un email con un rol definido. |
| RefreshToken | `refresh_tokens` | ID de usuario | Renovación y control de sesiones. |
| PasswordResetToken | `password_reset_tokens` | ID de usuario como clave primaria | Token de recuperación y vencimiento. |
| PasswordResetQuota | `password_reset_quotas` | Clave de cuota | Ventana temporal y contador de solicitudes. |

### Obra y operación

| Entidad | Tabla | Relación | Qué guarda |
| --- | --- | --- | --- |
| Proyecto | `proyectos` | Empresa mediante `empresa_id`; subcontratistas mediante tabla puente | Datos de la obra, fechas, superficies, presupuesto y configuración laboral propia. |
| Planta | `plantas` | Proyecto | Plantas o niveles de la obra. |
| RubroObra | `rubros_obra` | Proyecto; rubro padre, contrato y subcontratista opcionales | Jerarquía y datos de rubros y actividades. |
| ContratoObra | `contratos_obra` | Proyecto; subcontratista opcional | Contratos de la obra. |
| PresupuestoLinea | `presupuesto_lineas` | Proyecto | Partidas del presupuesto. |
| RestriccionObra | `restricciones_obra` | Proyecto; rubro opcional | Restricciones y estado de seguimiento. |
| RestriccionComentario | `restriccion_comentarios` | RestriccionObra | Comentarios de una restricción. |
| ObraTarea | `obra_tareas` | Proyecto; actividad o rubro opcional | Avance, cantidades, horas por categoría y cumplimiento. |
| RegistroHoras | `registros_horas` | Proyecto | Intervalos de trabajo, duración y datos de ejecución. |
| Entrega | `entregas` | Proyecto | Entregas de la obra. |
| Tarea | `tareas` | Entrega | Tareas de una entrega. |
| CostoIndirecto | `costos_indirectos` | Proyecto | Costos indirectos de obra. |
| Ingreso | `ingresos` | Proyecto | Ingresos de obra. |
| ReporteEmitido | `reportes_emitidos` | Proyecto | Reportes emitidos y su numeración por obra. |
| ReporteImagen | `reporte_imagenes` | Proyecto; reporte opcional | Referencia de imagen y metadatos como fecha, rubro y nivel. |
| CertificadoObra | `certificados_obra` | Proyecto; subcontratista opcional | Certificaciones, estado y datos de emisión o cierre. |
| CertificadoItemCompletado | `certificado_items_completados` | CertificadoObra y RubroObra | Ítems marcados como completados en un certificado. |
| Rendimiento | `rendimientos` | Sin asociación JPA a Proyecto o Empresa | Referencias de rendimiento y costo de mano de obra. |

### Facturación de la plataforma

| Entidad | Tabla | Relación | Qué guarda |
| --- | --- | --- | --- |
| PlanSuscripcion | `planes_suscripcion` | Referenciado por Suscripcion | Catálogo de planes y referencia de precio de Stripe. |
| Suscripcion | `suscripciones` | Usuario o empresa según titularidad; plan | Estado de suscripción y referencias de Stripe. |
| PagoSuscripcion | `pagos_suscripcion` | Suscripcion | Facturas, pagos y su estado. |
| StripeEventoProcesado | `stripe_eventos_procesados` | ID del evento Stripe como clave primaria | Registro de eventos procesados para evitar reprocesarlos. |

## Tablas auxiliares e históricas

| Tabla | Modelo asociado | Motivo |
| --- | --- | --- |
| `proyecto_subcontratistas` | Proyecto ↔ CatalogoEmpresa | Un proyecto puede tener varios subcontratistas del catálogo y el mismo subcontratista puede participar en varias obras. |
| `usuario_empresa_accesos` | UsuarioEmpresa → empresas asignadas | Guarda el conjunto de accesos adicionales por empresa. |
| `usuario_obra` | UsuarioEmpresa → obras asignadas | Guarda las obras asignadas a un usuario. |
| `invitacion_empresas` | InvitacionEquipo → empresas | Guarda las empresas incluidas en una invitación. |
| `invitacion_usuario_obras` | InvitacionUsuario → obras | Guarda las obras que se asignarán al aceptar. |
| `devoluciones_pago` | PagoSuscripcion → DevolucionPago | Guarda una colección de devoluciones. DevolucionPago es un @Embeddable. |
| `calendarios_laborales_empresa` | Histórica desde V31 | Conserva las filas anteriores al corte. El calendario activo vive en `empresas`. |
| `gastos_fijos` | Histórica | El módulo global se retiró. La tabla permanece en las migraciones y en el respaldo H2 cuando existe. |
| `altas_restriccion_chat` | Histórica | Permanece en las migraciones y en el respaldo H2 cuando existe. |
| `vinculos_telegram` | Histórica | Permanece en las migraciones y en el respaldo H2 cuando existe. |

## Puntos a revisar para simplificar

| Candidato | Evidencia en el modelo | Decisión pendiente |
| --- | --- | --- |
| ObraTarea y RegistroHoras | Comparten campos de cantidades, horas y cumplimiento, con distinta granularidad temporal. | Rastrear las escrituras y cálculos antes de elegir una fuente por dato. |
| Tablas históricas de Telegram y chat | Sus clases JPA ya se retiraron. | El retiro físico requiere una decisión explícita sobre los datos y la compatibilidad de los respaldos. |

Fuentes del inventario: [modelos Java](../src/main/java/com/vortise/gestion/domain/model), [Proyecto.java](../src/main/java/com/vortise/gestion/domain/model/Proyecto.java) y [migraciones SQL](../src/main/resources/db/migration).
