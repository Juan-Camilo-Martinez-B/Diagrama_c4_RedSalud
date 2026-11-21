package com.redsaludandina.compartido.aplicacion;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Almacén genérico en memoria para armar repositorios de prueba. */
public final class AlmacenEnMemoria<ID, T> {

    private final Map<ID, T> datos = new LinkedHashMap<>();
    private final Function<T, ID> identidad;

    public AlmacenEnMemoria(Function<T, ID> identidad) {
        this.identidad = identidad;
    }

    public Optional<T> buscar(ID id) {
        return Optional.ofNullable(datos.get(id));
    }

    public void guardar(T valor) {
        datos.put(identidad.apply(valor), valor);
    }

    public Collection<T> todos() {
        return datos.values();
    }
}
