# ADR-003 — Idempotencia del borde y política de conflicto

## Estado

Aceptado.

## Contexto

Cinco centros pierden el enlace a diario. El cliente reintenta el mismo lote, a veces en otro orden y contra otra instancia. La idempotencia tiene que sobrevivir al reinicio del proceso. Lo asistencial puede fusionarse. Los cupos y las recetas firmadas no.

## Decisión

El dispositivo genera el ULID del agregado y envía `Idempotency-Key`. El servidor guarda la clave, la huella SHA-256 del lote y la respuesta en `episode.sync_receipt`. La clave es la restricción única. El mismo lote devuelve la misma respuesta. Otro cuerpo con la misma clave responde 422.

La huella no usa el `toString` de `Object` ni el que genera el compilador para un record. Cada cambio define su `toString` con un formato fijo, y el lote se ordena por agregado y secuencia antes de hashear. Así dos instancias producen la misma huella.

Lo clínico se resuelve por last-writer-wins con el sello del centro. Los dos valores viajan en `ClinicalValueDiscarded`. Cupos y recetas firmadas se rechazan en el borde (`REQUIRES_SERVER`) y se resuelven contra el servidor. La agenda usa lock pesimista para no reservar dos veces el mismo cupo.

Los eventos salen por transactional outbox. En AWS la cola es SQS FIFO con grupo `contexto:agregado`.

## Consecuencias

Un reintento reordenado no se confunde con otro lote. La fusión clínica no borra el valor perdedor.
