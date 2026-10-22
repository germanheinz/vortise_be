# 005 — Precios oficiales BASIC y PRO

Estado: aprobado para implementación (2026-10-22). Alcance: solo `gestion_be`.

## Decisiones aprobadas

- BASIC: USD 9/mes (`900` centavos); PRO: USD 19/mes (`1900` centavos).
- Los cambios aplican a nuevas suscripciones. No se actualizan registros `Suscripcion` ni se invoca Stripe.
- El checkout usa el `stripePriceId` persistido en el plan. Solo se configurarán los nuevos recurring Price IDs provistos por el operador mediante `STRIPE_PRICE_BASIC` y `STRIPE_PRICE_PRO`.
- En una base vacía se crean ambos planes con los importes nuevos y los beneficios actuales, que siguen siendo informativos. El ID se establece al valor de entorno configurado o queda `null`.
- En una base inicializada se consultan únicamente BASIC y PRO. Para el precio legacy exacto BASIC `100` o PRO `200` centavos, se actualizan importe a `900`/`1900`, moneda a `usd` y Price ID al nuevo ID configurado; si falta la variable, se limpia el Price ID anterior (`null`) para deshabilitar checkout en vez de cobrar el precio antiguo.
- Si un plan ya tiene el importe oficial nuevo pero no tenía Price ID configurado en un arranque anterior, un arranque posterior debe asociar el ID de entorno configurado. Si no existe ID, permanece sin checkout; no se crea ni modifica Stripe remotamente.
- Filas BASIC/PRO con importe distinto del valor legacy se consideran configuración manual: se preservan importe y Price ID. No se les aplica un ID oficial de entorno porque podría diferir del importe que muestra la aplicación. El catálogo no reescribe nombre, trial, nivel, beneficios ni estado de esas filas.
- Planes ausentes solo se crean si la política global indica base vacía. La migración de los dos planes reconocidos se ejecuta también en bases pobladas, aun cuando esa política impida seeders de demo.
- No se crean ni modifican Products o Prices de Stripe, ni se hacen llamadas remotas.

## Compatibilidad y límites

Las suscripciones Stripe ya existentes continúan con el recurring Price asociado cuando se crearon; cambiar el catálogo local no cambia esos contratos. Solo checkouts nuevos leen el ID nuevo. El operador debe crear los recurring Prices BASIC y PRO en Stripe Dashboard y configurar los IDs correspondientes antes de habilitar checkout. Este cambio no verifica ni afirma que el precio remoto coincida con el local; sin ID configurado, los planes legacy migrados quedan sin checkout disponible.

No se cambian datos de suscripciones guardadas, esquema, H2 ni otros catálogos. No se siembran datos demo.

## Criterios de aceptación

1. Seed fresh crea BASIC/PRO a 900/1900 centavos, conserva trials y beneficios, y no llama al gateway Stripe.
2. En DB poblada, solo BASIC/PRO legacy exactos 100/200 pasan a 900/1900; con IDs configurados almacenan los IDs provistos, sin incluirlos en logs.
3. Sin una variable correspondiente, la fila migrada no conserva su Price ID legacy y el log identifica el nombre de la variable faltante sin imprimir valores.
4. Planes manuales no target conservan importe y Price ID, aunque haya variables de entorno configuradas.
5. Política de DB poblada impide crear planes ausentes, pero no impide la migración legacy de planes encontrados. Otros seeders siguen sin cambios.
6. Seeder no depende de un repositorio de suscripciones y no interactúa con Stripe.
