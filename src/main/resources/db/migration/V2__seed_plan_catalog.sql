-- Approved local catalog only. Stripe IDs remain NULL and are linked later from explicit environment configuration.
INSERT INTO planes_suscripcion (
    codigo, nombre, precio_mensual_centavos, moneda, dias_prueba, nivel, beneficios, stripe_price_id, activo
) VALUES
    (
        'BASIC', 'Basic', 900, 'usd', 7, 1,
        E'Gestión de empresas y obras\nCronograma y Gantt\nRestricciones y tareas\nRegistro de horas\nDashboard de obra\n7 días de prueba gratis',
        NULL, TRUE
    ),
    (
        'PRO', 'Pro', 1900, 'usd', 0, 2,
        E'Todo lo incluido en Basic\nReportes y rendimientos\nImportación desde Excel\nGastos fijos y cashflow\nSoporte prioritario',
        NULL, TRUE
    )
ON CONFLICT (codigo) DO NOTHING;
