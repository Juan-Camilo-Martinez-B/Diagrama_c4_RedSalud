package com.redsaludandina.legado.infraestructura.acl.cliente.soap;

import com.redsaludandina.legado.infraestructura.acl.modelo.HistorialLegado;
import java.time.Instant;
import java.util.List;

/**
 * Servicio SOAP nocturno del legado (Caso §3). Habla solo el modelo del legado.
 *
 * <p>PENDIENTE: la implementación se genera a partir del WSDL del legado, que el caso no incluye.
 * Mientras no exista, el job nocturno no corre y el read model se llena con las consultas
 * directas a la base de solo lectura.
 */
public interface ClienteSoapLegado {

    List<HistorialLegado> historialesActualizadosDesde(Instant desde);
}
