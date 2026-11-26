# Plan de refactor del backend

## Piloto de Previsto vs. Real

1. Confirmar el diff local de `GetPrevistoRealUseCase` como línea base. Antes del refactor, revisar la cobertura y añadir pruebas de caracterización donde falte evidencia. Priorizar precedencia de días, nulos frente a cero, valores no finitos, dotación heredada y orden de rubros y cargas.
2. Hacer que `execute` muestre la secuencia de lectura del proyecto, resolución de jornada y costos, agrupación por rubro y construcción del resumen. Usar imports explícitos y nombres como `costoHoraOficial` y `tareasPorRubro`. Extraer métodos privados para las etapas que hoy están mezcladas dentro del stream.
3. Extraer cálculo y agregación del anexo a una clase de paquete sin dependencias de Spring ni repositorios. Mantener los records públicos, constructores y la firma de `execute`. Reutilizar `PrevistoRealCalculator` y `EstructuraRubros`. Conservar la posición y frecuencia de lectura de la fecha, la suma en el mismo orden y el comportamiento del agrupamiento con claves nulas. Mantener el acceso a datos dentro de la transacción existente.
4. Probar primero el caso de uso y sus consumidores directos. Revisar el diff comparando las ramas y operaciones con la línea base. Verificar vía HTTP la respuesta y los accesos y ejecutar la verificación completa según la aprobación de ejecución. Cerrar con revisión independiente.

## Candidatos posteriores

| Orden propuesto | Evidencia | Siguiente corte |
| --- | --- | --- |
| 2 | `CreateObraTareaUseCase` repite cálculo de métricas en alta y edición. | Caracterizar ambas rutas y extraer el cálculo compartido conservando validaciones, bloqueos y redondeo. |
| 3 | `UpdateProyectoUseCase` reúne actualización, cambios de empresa, estructura y contratistas. | Revisar el flujo transaccional y extraer una responsabilidad por cambio, empezando por el mapeo de rubros y sus validaciones. |
| 4 | `CertificadoObraUseCase` reúne consultas, armado de detalle, edición de corte y cierre. | Separar primero la composición de vistas después de caracterizar resultados por empresa, bloqueos y estados. |
| 5 | `VincularTelegramUseCase` importa directamente `JpaVinculoTelegramRepository`. | Revisar un puerto específico con el patrón existente y preservar consultas, bloqueos y semántica transaccional. Aprobación independiente para ese cambio. |

El dominio contiene modelos anotados con JPA y el proyecto ya tiene puertos. La extracción de puertos se propone donde existe una dependencia concreta hacia infraestructura. Separar masivamente entidades de dominio y persistencia queda fuera de este plan por su impacto en mappings, carga de relaciones y persistencia.

## Segundo corte propuesto

Extraer un método privado para aplicar los datos compartidos después de validar la actividad y otro para derivar las métricas de avance. Mantener explícitas en las entradas las validaciones específicas y la asignación de metadatos del alta. Conservar la secuencia de consultas y mutaciones del bloque común en este corte. Limitar los cambios a `CreateObraTareaUseCase.java` y las pruebas de caracterización que falten.
