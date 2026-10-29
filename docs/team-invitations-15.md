# Invitaciones de equipo

Un titular ADMINISTRADOR con Pro puede invitar hasta cinco colaboradores activos en total. Una cuenta invitada puede tener varias empresas asignadas y consume una sola plaza. Las empresas deben pertenecer al titular y estar cubiertas por Pro: su Pro personal cubre sus empresas; un Pro empresarial cubre únicamente esa empresa.

COLABORADOR no crea empresas u obras (incluida importación Excel), no gestiona usuarios/invitaciones y no accede a facturación. Conserva las operaciones de obra autorizadas en empresas asignadas. Las membresías y cobertura se consultan en servidor, no se confía en IDs o rol enviados por el navegador. La sesión mantiene su contrato y devuelve el nuevo valor de rol.

## Contratos

- POST `/api/equipo/invitaciones` con `{empresaIds:[...]}` devuelve201 `{id,enlace,venceEn,empresas:[{id,nombre}]}`. Lista no vacía, máximo100 IDs de empresas propias cubiertas; se normalizan duplicados. El enlace se devuelve solo al generar.
- GET misma ruta devuelve `{invitaciones:[{id,venceEn,estado,empresas}],invitadosActivos,limite:5,puedeInvitar,empresasDisponibles}` sin tokens.
- DELETE misma ruta `/{id}` revoca invitación pendiente del titular; no necesita mantener Pro para revocar.
- POST `/api/auth/invitaciones/consultar` con `{token}` devuelve empresas/vencimiento de enlace vigente.
- POST `/api/auth/invitaciones/aceptar` con `{token,nombre,email,password}` crea solo COLABORADOR, consume la invitación y devuelve201 con sesión. No reasigna cuentas con email existente ni acepta titular/rol/empresas desde el cliente.
- El POST histórico `/api/empresas/{id}/usuarios` devuelve409 indicando usar invitación. Edición existente conserva nombre/contraseña y roles históricos; un invitado no se promueve mediante esa edición. Reactivar aplica mismo cupo/plan y lock de titular.

Token aleatorio256bits, hash SHA256 persistido, vencimiento72h y un solo consumo. Aceptación/revocación/altas concurrentes se serializan por titular y luego invitación. Las invitaciones pendientes no reservan plazas. Email duplicado, fallo de contraseña o cupo no consumen el enlace ni dejan cuentas parciales.

Sin cobertura Pro los invitados reciben403 `TEAM_PRO_REQUIRED`; no se borra/desactiva su cuenta por ello. Al recuperar Pro vuelve a ser válido su acceso mientras sesión/cuenta sigan vigentes. Si expira solo un Pro empresarial, se deniega esa empresa y se conservan las restantes; el listado y empresa inicial también se ajustan.

## Persistencia y pruebas

V10 añade `invitaciones_equipo`, `invitacion_empresas`, `usuario_empresa_accesos` y vínculo nullable `usuarios_empresa.invitante_id`. V1–V9 intactas. Usuarios históricos no se reclasifican ni borran. Eliminar una empresa con accesos compartidos o un titular con invitados se rechaza para evitar eliminar cuentas de otras empresas. Cambiar asignaciones después de aceptar no forma parte de esta entrega.

Contextos optativos: TeamInvitationsHttpTest usa H2 UUID protegido; TeamInvitationsPostgresTest requiere `team15.pg.enabled=true` y harness `python3 -B scripts/team-postgres.py`: nueva DB PostgreSQL18 tmpfs en127.0.0.1:15439, nunca bases existentes. `mvn verify` ordinario mantiene optativos desactivados. Aplicar checklist de ejecución y autorización antes de abrir contextos.

El perfil e2e-local incorpora fixtures sintéticas propias bajo guard pre-DataSource. APP_E2E_TEAM_PASSWORD lo genera el harness frontend; no es variable requerida en producción ni debe configurarse con contraseñas reales. Sin correo ni llamadas Stripe en fixtures. Evidencia y revisión: `docs/specs/TEAM-INVITATIONS-15/tasks.md` del workspace.
