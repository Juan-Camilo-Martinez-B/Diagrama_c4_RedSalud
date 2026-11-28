# ADR-001 — Límites de módulo en el monolito

## Estado

Aceptado.

## Contexto

RedSalud Andina tiene un equipo pequeño, 14 centros y 9 especialistas. Partir el sistema en microservicios no mejora la disponibilidad que ya da un despliegue Multi-AZ, y encarece la operación. El caso pide reutilizar al menos el 80 % del dominio si aparece un canal nuevo.

## Decisión

Un monolito modular con siete contextos: identidad, agenda, episodio, receta, interconsulta, legado e insights (auditoría y analítica). Cada uno es dueño de su esquema Flyway. La comunicación entre contextos es por eventos. El único contrato síncrono permitido es el comando de autorización de Identidad.

ArchUnit rompe el build si un contexto usa el interior de otro, si el dominio depende de Spring o JPA, o si una consulta toca el lado de escritura.

La señalización de video no vive en el monolito: el caso la extrae porque su latencia (≤ 300 ms) es otro atributo de calidad.

## Consecuencias

Un cambio de canal reutiliza los mismos comandos. Extraer un contexto más adelante es mover un paquete y su esquema, no reescribir el dominio.
