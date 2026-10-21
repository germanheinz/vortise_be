# 003 — Plan técnico

Status: DRAFT — pending approval. See [spec.md](spec.md).

## Decisions
- **Dependency:** add `com.stripe:stripe-java` (official SDK) to `gestion_be/pom.xml`.
  No frontend Stripe dependency: Checkout and Portal are hosted redirects.
- **Config (env only, never committed):** `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`,
  `APP_FRONTEND_URL` (success/cancel/portal return URLs), `APP_UPLOADS_DIR`. Bound via
  `application.yml` placeholders with empty defaults; a `StripeGateway` reports `isConfigured()`.
  Publishable key not needed (hosted Checkout). Locally via shell env or an untracked
  `gestion_be/.env` excluded by `.gitignore`; on Render via dashboard env vars.
- **Stripe Price IDs:** the seeder stores plans with `stripePriceId = null`. When keys are
  provided you either paste existing Price IDs via env (`STRIPE_PRICE_PRO`,
  `STRIPE_PRICE_PRO_PLUS`), or the seeder creates Product+Price in Stripe once if keys
  exist and the ID is missing (lookup by `lookup_key` to stay idempotent).
- **Trial** applied at checkout (`subscription_data.trial_period_days`) from the plan row,
  so it can change without a new Stripe price.
- **PRO computation** server-side only; JWT does not carry `pro` (it would go stale).
- **Photo storage:** `FotoPerfilStorage` port + `FileSystemFotoPerfilStorage` adapter writing
  `{APP_UPLOADS_DIR}/{usuarioId}.{ext}` with normalized path check; swappable later for a
  storage service. `UsuarioEmpresa` gets nullable `fotoContentType` (non-destructive column).
- **Holder model:** `SuscripcionTitular` = USUARIO | EMPRESA; subscription row references
  either usuario or empresa. `PlanEfectivoService` resolves best active plan.
- **Webhook security:** raw body + `Stripe-Signature` verified with `Webhook.constructEvent`;
  `/api/stripe/webhook` is permitAll and excluded from tenant interceptor; processed
  event ids stored to make handling idempotent. Customer→user mapping via
  `stripeCustomerId` stored on the subscription row and `client_reference_id`/metadata `usuarioId`.
- **Schema:** new tables only, created by existing `ddl-auto: update`. No destructive changes.

## Backend (gestion_be)
Domain/model:
- `PlanSuscripcion` (codigo unique, nombre, precioMensualCentavos, moneda, diasPrueba,
  beneficios text, stripePriceId, activo)
- `Suscripcion` (titular, usuario nullable, empresa nullable, plan, stripeCustomerId,
  stripeSubscriptionId, estado, finPrueba, finPeriodoActual, cancelaAlFinal, actualizadoEn)
- `PagoSuscripcion` (suscripcion, stripeInvoiceId unique, montoCentavos, moneda, estado, fecha)
- `StripeEventoProcesado` (eventId PK, tipo, procesadoEn)
- Repository interfaces + JPA adapters following existing persistence pattern.

Application:
- `SuscripcionUseCase` (planes, mia, checkout, portal, isPro)
- `StripeWebhookUseCase` (event dispatch, idempotency)
- `PerfilUseCase` (me, foto upload/get/delete with magic-byte validation)
- `RegistroUseCase` (empresa + ADMINISTRADOR in one transaction, returns login payload)
- Extend `AutenticacionUseCase`/`LoginResponseDto` and `UsuarioEmpresaUseCase`/DTO.
- `StripeGateway` port + `StripeSdkGateway` adapter (only class touching the SDK).

Infrastructure:
- `SuscripcionController`, `StripeWebhookController`, `PerfilController`, `AuthController` (+registro).
- `SecurityConfig`: permit `/api/stripe/webhook`.
- `PlanSuscripcionSeeder` (@Order after UsuarioDemoSeeder), idempotent by codigo.
- `GlobalExceptionHandler`: map "not configured" to 503 (new small exception).
- `.gitignore`: add `.env` and `data/uploads/` before any secret or photo exists locally.

## Frontend (gestion_fe)
- `types/index.ts`: `PlanSuscripcionDto`, `MiSuscripcionDto`, `PerfilDto`, extend
  `UsuarioEmpresaDto` with optional `suscripcion`.
- `lib/api.ts`: `suscripcionesApi`, `perfilApi` (photo fetched as blob → object URL).
- `app/planes/page.tsx`: two plan cards, benefits, trial badge, subscribe/manage buttons,
  loading/error/disabled states; reads `?estado=exito|cancelado` after Checkout.
- `components/layout/ProfileAvatar.tsx`: photo or initials, upload/remove menu,
  PRO badge underneath; placed in `AppLayout` sidebar header; link to /planes.
- Login: store new session fields; layout refreshes `/api/me` on mount.
- `app/registro/page.tsx` + link from login; layout treats `/registro` like `/login`.
- `app/empresas/[id]/usuarios/page.tsx`: subscription columns.

## Tests
- Context-free JUnit/Mockito: PRO computation by status/dates, checkout validation
  (unknown plan, already active, trial days), webhook dispatch + idempotency + invalid
  signature, photo validation (size, magic bytes, ownership), seeder idempotency,
  admin DTO mapping.
- HTTP integration (isolated H2, needs execution approval): 403 for non-admin on users
  status, webhook permitAll + signature rejection, photo endpoints own-user only.
- Frontend: `tsc --noEmit`; `npm run build` run by you.

## Risks
- Webhooks cannot reach localhost without `stripe listen` (Stripe CLI) — manual test step.
- Filesystem photos on Render are lost on redeploy until a storage service is added.
- Public registration without email verification or rate limiting: acceptable for demo only.
- Stripe secret key was shared in chat: must be rolled before real use.
- `ddl-auto: update` adds tables to the persistent H2 on next start (non-destructive).
