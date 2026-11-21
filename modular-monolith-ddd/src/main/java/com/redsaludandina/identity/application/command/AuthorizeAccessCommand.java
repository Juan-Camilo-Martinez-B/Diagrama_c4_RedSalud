package com.redsaludandina.identity.application.command;

import com.redsaludandina.shared.application.cqrs.Command;

/**
 * Servicio de host abierto (OHS) de Identidad: «este usuario quiere acceder a este recurso». Si la
 * política lo niega, el intento queda registrado y el comando falla con
 * {@link com.redsaludandina.shared.application.ForbiddenAccessException}.
 *
 * <p>Se debe ejecutar FUERA de la transacción del caso de uso que lo llama, para que la negación
 * quede confirmada aunque ese caso de uso falle.
 */
public record AuthorizeAccessCommand(String userId, String resource, String patientId, String centerId,
                                     String specialty) implements Command {
}
