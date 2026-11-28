# ADR-002 — Capa anticorrupción y vista de lectura del legado

## Estado

Aceptado.

## Contexto

El legado no se modifica. Expone una base de solo lectura y un SOAP nocturno cuyo WSDL el caso no entrega. El especialista debe leer el historial en p95 ≤ 2 s con caché y ≤ 6 s en consulta directa. Cambiar de proveedor debe tocar un solo módulo.

## Decisión

El núcleo depende del puerto `LegacyHistorySource`. Solo el adaptador habla con el legado y traduce a un modelo canónico. La consulta lee una copia propia con marca de frescura (`X-Data-Freshness`). Si la copia caducó, se consulta el legado con timeout de 6 s. Si el legado no responde y hay copia vieja, se conserva esa copia.

Resilience4j abre el circuito cuando el legado falla seguido, y esa falla se trata como legado no disponible.

La caché tiene dos niveles: Caffeine en la instancia y un puerto de caché compartida. En local ese puerto es en proceso. En AWS el mismo puerto lo cumple ElastiCache, sin cambiar el dominio. La copia durable sigue en el esquema `legacy` de Aurora (ADR-01: Aurora para el dominio transaccional; DynamoDB no sustituye esa copia).

El cliente SOAP queda como interfaz hasta que exista el WSDL. El job nocturno no arranca sin él.

## Consecuencias

Un proveedor nuevo se cambia dentro de `legacy.infrastructure.acl`. El dominio no se entera.
