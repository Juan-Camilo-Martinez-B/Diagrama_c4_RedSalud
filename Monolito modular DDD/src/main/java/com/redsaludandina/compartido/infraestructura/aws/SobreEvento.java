package com.redsaludandina.compartido.infraestructura.aws;

import java.time.Instant;
import java.util.UUID;

/**
 * Cuerpo del mensaje SQS: el sobre del outbox con el evento ya serializado en {@code payload}.
 * Viaja el {@code eventoId} para que cada consumidor descarte duplicados.
 */
public record SobreEvento(UUID eventoId, String contexto, String agregadoId, String tipo, String payload,
                          Instant registradoEn) {
}
