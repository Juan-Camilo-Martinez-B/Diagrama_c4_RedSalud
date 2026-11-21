package com.redsaludandina.legacy.infrastructure.acl.client.soap;

import com.redsaludandina.legacy.infrastructure.acl.model.LegacyHistory;
import java.time.Instant;
import java.util.List;

/**
 * Servicio SOAP del legado que alimenta el job nocturno (Caso §3). PENDIENTE: el caso no entrega el
 * WSDL; cuando exista se implementa esta interfaz y el job se habilita con
 * {@code redsalud.legacy.job.enabled=true}.
 */
public interface LegacySoapClient {

    List<LegacyHistory> historiesUpdatedSince(Instant since);
}
