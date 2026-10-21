# 003 — Suscripciones Stripe, foto de perfil y estado PRO

Status: DRAFT — pending approval. Cross-repo (gestion_be + gestion_fe).

## Scope
1. Monthly Stripe subscription, two plans (BASIC, PRO), symbolic prices; BASIC has a 7-day free trial.
2. Plans page listing both plans with their benefits and a subscribe button.
3. User covered by an active subscription shows a plan badge (BASIC or PRO) under the profile photo.
4. Profile photo upload; retrieved on login and shown in the app layout.
5. Company administrators see their users' and company's subscription/payment status.
6. Seeder creates the plan catalog required to operate payments.
7. Public self-registration: creates a new company with the user as ADMINISTRADOR.

Out of scope: access restrictions (no feature gating now), annual billing, coupons,
taxes/invoicing, refunds, multiple currencies, email verification, password reset.

## Behavior
- Subscription holder is either a user (`USUARIO`) or a whole company (`EMPRESA`).
  - Any user may subscribe individually for themselves.
  - Only an ADMINISTRADOR may subscribe their own company; it covers all its active users.
  - Effective plan of a user = best active plan among own and company subscription (PRO > BASIC).
- Plans (seeded, editable later):
  - `BASIC` — USD 1/month symbolic, 7-day free trial. Benefits: obras y empresas,
    cronograma y Gantt, restricciones, registro de horas, dashboard de obra.
  - `PRO` — USD 2/month symbolic, no trial. Benefits: todo lo de BASIC, reportes y
    rendimientos, importación Excel, gastos fijos/cashflow, soporte prioritario.
  Benefits are informational only; nothing is restricted yet.
- Registration: public form (empresa nombre, nombre, email, password ≥ 8). Creates a
  company and its ADMINISTRADOR user, returns the same payload as login. Duplicate
  email or company name → 400 with a generic message. Respects company user limit rules.
- Subscribing redirects to Stripe Checkout (hosted). Card data never touches our servers.
- Stripe is the source of truth; local state is updated only from signature-verified webhooks.
- Active = local subscription status `trialing` or `active` and current period not expired.
  `past_due`, `unpaid`, `canceled`, `incomplete*` → not PRO.
- A subscribed user can open the Stripe Customer Portal to cancel or update their card.
- Non-subscribed users keep full access; they see a "Mejorar a PRO" link.
- One active subscription per holder; subscribing again while active is rejected
  (plan changes go through the Customer Portal).
- Without Stripe keys configured: plans page shows plans, button disabled with
  "Pagos aún no configurados"; payment endpoints return 503. Nothing else breaks.

### Profile photo
- Authenticated user uploads/replaces/deletes only their own photo.
- JPEG, PNG or WEBP, max 2 MB, validated by content (magic bytes), not only extension.
- Stored on the app filesystem under configurable `APP_UPLOADS_DIR` (default `./data/uploads/avatars`),
  server-generated file name per user id (never the client file name). Temporary until a
  storage service exists; on Render the disk is ephemeral and photos may be lost on redeploy.
- Login response and `GET /api/me` indicate whether a photo exists; the frontend
  fetches it with the Bearer token and shows initials when absent.
- Plan badge (BASIC/PRO) appears under the photo in the sidebar header.

### Admin view
- Only `ADMINISTRADOR`, only for users of their own company (existing tenant rule).
- Users page adds: plan, subscription status, trial end, next renewal, last payment
  date/amount and result. Read-only; no payment actions on behalf of users.

### Tenant authorization (prerequisite for public registration) — APPROVED 2026-10-21
User decision: ADMINISTRADOR sees only the companies they created; SUPER_ADMIN sees all.
- SUPER_ADMIN = `UsuarioEmpresa.superAdmin` flag (rol stays ADMINISTRADOR) exposed as
  `ROLE_SUPER_ADMIN` via a JWT claim. Not an enum value, to avoid altering the existing
  H2 enum check constraint on `rol`. Never assignable via API or registration.
- `Empresa.creadoPorUsuarioId` (nullable column). Companies created by an admin (registration
  or POST /api/empresas) record the creator.
- ADMINISTRADOR may access company X if X is their own company or `X.creadoPorUsuarioId == own id`.
  JEFE_DE_OBRA/RESPONSABLE/CONSULTA: only their own company (unchanged).
- Existing companies have no creator: only reachable by their own users and SUPER_ADMIN.
  The existing demo admin therefore stops seeing other companies.
- SUPER_ADMIN is created only by the seeder from env (`APP_SUPERADMIN_EMAIL`,
  `APP_SUPERADMIN_PASSWORD` ≥ 12 chars) in an internal "Plataforma" company; without env
  no SUPER_ADMIN exists. An existing non-super user with that email is never promoted
  (prevents takeover by pre-registering the address).
- Role/flag changes take effect on next login (JWT lifetime, 24 h by default).
- Applies to `CurrentTenantService.requireEmpresa`, `GetEmpresasUseCase`, `@PreAuthorize`
  admin checks (SUPER_ADMIN also allowed), and the new subscription/admin endpoints.
- Existing HTTP authorization tests are updated to the new rule.

## API contract (new or changed)
| Method | Path | Auth | Response |
|---|---|---|---|
| POST | /api/auth/login | public | adds `suscripcionActiva: boolean`, `planCodigo: string|null`, `tieneFoto: boolean` |
| POST | /api/auth/registro | public | body `{ empresaNombre, nombre, email, password }` → login payload |
| GET | /api/me | user | `{ usuarioId, nombre, email, rol, empresaId, empresaNombre, suscripcionActiva, planCodigo, origenPlan: USUARIO|EMPRESA|null, tieneFoto }` |
| GET | /api/suscripciones/planes | user | `[{ codigo, nombre, precioMensual, moneda, diasPrueba, beneficios[], disponible }]` |
| GET | /api/suscripciones/mias | user | `{ usuario: Suscripcion|null, empresa: Suscripcion|null }` |
| POST | /api/suscripciones/checkout | user | body `{ planCodigo, titular: USUARIO|EMPRESA }` → `{ url }`; EMPRESA requires ADMIN |
| POST | /api/suscripciones/portal | user | body `{ titular }` → `{ url }` |
| POST | /api/stripe/webhook | public, Stripe signature required | 200 |
| PUT | /api/me/foto | user | multipart `file` → 204 |
| GET | /api/me/foto | user | image bytes, 404 if none |
| DELETE | /api/me/foto | user | 204 |
| GET | /api/empresas/{id}/usuarios | ADMIN | each item adds `suscripcion: { planCodigo, estado, activa, origen, finPrueba, finPeriodoActual, ultimoPagoFecha, ultimoPagoMonto, ultimoPagoEstado } | null` |
| GET | /api/empresas/{id}/suscripcion | ADMIN | company subscription in the same shape, or 204 |

Existing fields remain unchanged for compatibility.

## Acceptance criteria
- AC1 Plans page shows both plans, prices, benefits; trial label only on BASIC; admins
  can choose "para mí" or "para toda la empresa".
- AC2 Checkout creates a Stripe session for the chosen holder and plan; BASIC includes
  `trial_period_days=7`; unknown/inactive plan → 400; EMPRESA by non-admin → 403.
- AC3 Webhook with invalid signature → 400 and no state change; duplicate event ids are ignored.
- AC4 `checkout.session.completed`, `customer.subscription.*`, `invoice.paid`,
  `invoice.payment_failed` update subscription and payment records.
- AC5 Login and `/api/me` return the effective plan only for trialing/active own or company subscriptions.
- AC6 BASIC/PRO badge under the photo in the layout; absent for non-covered users.
- AC7 Photo upload rejects >2 MB and non-image content; user can only touch their own photo;
  photo is visible again after logout/login; no path traversal via file names.
- AC11 Registration creates an isolated company + ADMINISTRADOR and logs in; duplicates rejected.
- AC8 Admin sees subscription status only for their company's users; non-admins get 403.
- AC9 Without Stripe config the app starts and all existing features work.
- AC10 Seeder is idempotent: repeated startups do not duplicate plans or alter manual edits.
