-- Reporte reproducible: quién vio un recurso, quién fue rechazado y qué conflicto se fusionó.
-- Sustituir el recurso. En local la tabla es insights.access_record.
-- En AWS la misma pregunta lista el prefijo access-log/<recurso>/ del bucket con Object Lock.

SELECT user_id, type, reason, occurred_at, detail
FROM insights.access_record
WHERE resource = 'episode/01ARZ3NDEKTSV4RRFFQ69G5FAV'
ORDER BY occurred_at, id;
