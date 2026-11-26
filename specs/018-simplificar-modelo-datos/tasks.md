# Estado

- [x] Identificada la relación 1 a 1 de CalendarioLaboralEmpresa con Empresa y los consumidores de configuración, inicialización, baja, alta de obra y previsto vs. real.
- [x] Confirmadas dos entradas distintas en el frontend para avance y registro de horas.
- [x] Detectadas lecturas de listas completas por obra en ambos servicios.
- [ ] Aprobar alcance de la primera entrega y definir verificación en un entorno aislado.
- [ ] Implementar el calendario integrado, comprobar compatibilidad de la API y validar migración, baja y restauración.
- [ ] Medir consultas y evaluar la consolidación de datos de avance y horas.

## Primera entrega

- [x] «apruebo» autorizó cambios locales en el backend, la migración versionada y pruebas sin contexto Spring. La ejecución de migraciones sobre bases existentes, arranque de contextos y despliegues quedan fuera de esta aprobación.
- [x] Calendario integrado en Empresa, dos repositorios retirados, V31 preparada y documentación del modelo actualizada. El código registra 34 entidades.
- [x] Pasaron 41 pruebas con `mvn -Dtest=CompanyInitializationTest,CalendarioLaboralObraTest,PrevistoRealUseCaseTest,EmpresaConfiguracionUseCaseResponsableTest,EmpresaConfiguracionUseCaseUnidadTest,PostgresMigrationSourceTest clean test`. Pasó `git diff --check`.
- [ ] Pendientes la ejecución de V31 con datos previos, la verificación de baja, la restauración H2 y la revisión independiente. El perfil H2 persistente requiere un procedimiento de transición explícito.
