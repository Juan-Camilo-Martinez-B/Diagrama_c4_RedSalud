package com.redsaludandina.compartido.aplicacion.mensajeria;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Inbox de prueba en memoria. */
public final class InboxEnMemoria implements InboxPort {

    private final Set<String> vistos = new HashSet<>();

    @Override
    public boolean registrarSiEsNuevo(String consumidor, UUID eventoId) {
        return vistos.add(consumidor + "/" + eventoId);
    }
}
