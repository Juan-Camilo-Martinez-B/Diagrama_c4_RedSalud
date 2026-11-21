/**
 * AdaptadorHistorial: implementa HistorialClinicoPort. Lee primero el read model (p95 <= 2 s) y solo si falta consulta la BD de solo lectura (<= 6 s).
 */
package com.redsaludandina.legado.infraestructura.acl.adaptador;
