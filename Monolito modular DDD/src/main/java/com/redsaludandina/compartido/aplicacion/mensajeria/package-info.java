/**
 * Patrón OUTBOX transaccional y su contraparte INBOX.
 *
 * <ul>
 *   <li>Un caso de uso guarda su agregado y, en la misma {@code UnidadDeTrabajo}, los eventos en el
 *       outbox de su contexto ({@link com.redsaludandina.compartido.aplicacion.mensajeria.RegistradorOutbox}).</li>
 *   <li>{@link com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox} publica después los
 *       pendientes en el bus (EventBridge), en orden por agregado, y los marca publicados.</li>
 *   <li>La entrega es «al menos una vez»: cada consumidor tolera duplicados por el id del evento,
 *       con {@link com.redsaludandina.compartido.aplicacion.mensajeria.InboxPort} si no es idempotente
 *       por sí mismo.</li>
 * </ul>
 */
package com.redsaludandina.compartido.aplicacion.mensajeria;
