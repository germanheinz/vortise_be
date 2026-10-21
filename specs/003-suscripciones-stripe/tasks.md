# 003 — Tasks

Status: spec v1 approved in chat; scope expanded by user answers (registration, company
holder, BASIC/PRO, filesystem photos) — awaiting re-approval of updated spec/plan.
Stripe test keys received in chat; NOT stored anywhere. Secret key must be rolled.

- [x] Re-approval: updated spec + plan (2026-10-21; keys to be rotated by user before commit)
- [x] Approval: tenant authorization section (approved 2026-10-21; deliver it first)
- [x] Phase A — tenant authorization implemented
  - Empresa.creadoPorUsuarioId; UsuarioEmpresa.superAdmin (schema.sql ADD COLUMN IF NOT EXISTS, default FALSE)
  - JWT `superAdmin` claim → ROLE_SUPER_ADMIN; CurrentTenantService creator-aware
  - GetEmpresasUseCase scoped; SaveEmpresaUseCase records creator; SuperAdminSeeder (env only)
  - .gitignore: .env, /data/uploads/
  - PlantaAuthorizationHttpTest updated: ADMIN denied foreign; creator admin / super admin allowed
  - Evidence: `mvn test -Dtest=CurrentTenantServiceTest,EmpresaTenantScopeTest,SuperAdminSeederTest,JwtAuthenticationFilterTest,UsuarioDemoSeederTest,RubroPlanningTest,DataSeederTest,DemoSeederCoverageTest`
    → 35 tests, 0 failures/errors/skips, BUILD SUCCESS (2026-10-21). HTTP tests compiled, NOT run.
- [ ] Phase A HTTP verification: `mvn -Dtest=PlantaAuthorizationHttpTest,PrevistoRealHttpTest,RestriccionDetalleHttpTest test`
  - Run by user 2026-10-21: 95 tests, 94 pass, 1 error in RestriccionDetalleHttpTest.fixtures
    (unique empresas.nombre "Comparison A"). Pre-existing: committed fixtures reuse a fixed
    name across its two tests; file unchanged since HEAD ebe39a6. Not caused by Phase A.
  - Fix: randomized fixture company names. Re-run by user 2026-10-21: BUILD SUCCESS (user-reported).
- [x] Phase A closed (independent review still pending at feature closure)
- [x] Phase B — implemented (2026-10-21)
  - BE: stripe-java 33.4.2 (API dahlia; API verified with javap); PlanSuscripcion, Suscripcion,
    PagoSuscripcion, StripeEventoProcesado + repos; StripeGateway port / StripeSdkGateway;
    SuscripcionUseCase, SuscripcionConsultaService, StripeWebhookUseCase, PerfilUseCase,
    RegistroUseCase; FileSystemFotoPerfilStorage; PlanSuscripcionSeeder; controllers;
    webhook permitAll (signature-verified); 400/502/503/413 mappings; config via env.
  - BE tests added: SuscripcionUseCaseTest, StripeWebhookUseCaseTest, PerfilFotoTest,
    RegistroUseCaseTest, PlanSuscripcionSeederTest. HTTP tests: @MockBean PlanSuscripcionSeeder.
  - FE: types, perfilApi/suscripcionesApi/registro, saveSession (token no longer duplicated in
    gestion_session), ProfileAvatar (photo, BASIC/PRO badge under photo, logout), /planes,
    /registro, users page subscription status.
  - Evidence: FE `./node_modules/.bin/tsc --noEmit --incremental false` → exit 0.
    BE compile/tests NOT run yet (agent run skipped by user).
- [ ] BE focused tests: `mvn -Dtest='CurrentTenantServiceTest,EmpresaTenantScopeTest,SuperAdminSeederTest,JwtAuthenticationFilterTest,UsuarioDemoSeederTest,RubroPlanningTest,DataSeederTest,DemoSeederCoverageTest,SuscripcionUseCaseTest,StripeWebhookUseCaseTest,PerfilFotoTest,RegistroUseCaseTest,PlanSuscripcionSeederTest' test`
- [ ] BE full verification (approval needed): `mvn verify`
- [ ] FE build (user): `npm run build`
- [ ] Manual Stripe test with rotated test keys + `stripe listen`
- [ ] BE: .gitignore, model, repositories, seeder
- [ ] BE: registration
- [ ] BE: Stripe gateway, checkout/portal, webhook
- [ ] BE: perfil/foto, login + /api/me, admin users DTO
- [ ] BE: focused context-free tests
- [ ] FE: types, api, /planes, avatar + PRO badge, admin columns
- [ ] Verification (BE focused tests; HTTP tests + mvn verify need execution approval; FE tsc; build by user)
- [ ] Independent review and closure
