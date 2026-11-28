# ADR-004 — Auditoría demostrable

## Estado

Aceptado.

## Contexto

Toda lectura de historia clínica debe poder responder quién vio el documento, cuándo y con qué motivo. Una denegación también cuenta. El conflicto clínico debe conservar los dos valores. La prueba no es que exista un log: es poder consultarlo.

## Decisión

La autorización se evalúa en la aplicación, en `AuthorizeAccessHandler`, no en el controlador. Si niega, escribe `AccessDenied` en el outbox de identidad y la transacción hace commit antes de devolver 403.

Cada lectura emite `ClinicalRead`. Cada fusión emite `ClinicalValueDiscarded` con el valor conservado y el descartado. Insights los guarda en un almacén append-only. En local es la tabla `insights.access_record` (solo insert). En AWS es S3 Object Lock en modo compliance.

`GET /insights/access-log?resource=` devuelve lecturas, denegaciones y conflictos de ese recurso. El script reproducible está en `docs/audit/quien-vio.sql` y un ejemplo de salida en `docs/audit/salida-ejemplo.txt`.

El rol `ADMINISTRATOR` no tiene acceso clínico. Su intento queda como `ACCESS_DENIED`.

## Consecuencias

El reporte de un recurso muestra tanto el acceso concedido como el negado, y el detalle del conflicto cuando lo hubo.
