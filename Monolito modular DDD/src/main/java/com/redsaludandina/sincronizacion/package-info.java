/**
 * OHS de sincronizacion offline. No es un contexto delimitado: recibe los lotes de los centros rurales, deduplica por ULID y entrega cada cambio al modulo dueno. La politica de conflictos vive en cada modulo.
 */
package com.redsaludandina.sincronizacion;
