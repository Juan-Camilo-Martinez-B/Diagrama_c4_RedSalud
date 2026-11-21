package com.redsaludandina.compartido.infraestructura.mensajeria;

import com.redsaludandina.compartido.dominio.EventoDominio;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte los eventos del lenguaje publicado a JSON y de vuelta. Solo acepta los tipos
 * registrados: un tipo desconocido en el outbox es un error, no algo que se adivina.
 */
public final class SerializadorEventos {

    private final JsonMapper json;
    private final Map<String, Class<? extends EventoDominio>> tipos;

    public SerializadorEventos(JsonMapper json, Collection<Class<? extends EventoDominio>> tipos) {
        this.json = json;
        this.tipos = tipos.stream().collect(Collectors.toUnmodifiableMap(Class::getSimpleName, Function.identity()));
    }

    public String tipo(EventoDominio evento) {
        String tipo = evento.getClass().getSimpleName();
        if (!tipos.containsKey(tipo)) {
            throw new IllegalArgumentException("Evento no registrado en el lenguaje publicado: " + tipo);
        }
        return tipo;
    }

    public String aJson(EventoDominio evento) {
        tipo(evento);
        return json.writeValueAsString(evento);
    }

    public EventoDominio desdeJson(String tipo, String contenido) {
        Class<? extends EventoDominio> clase = tipos.get(tipo);
        if (clase == null) {
            throw new IllegalStateException("Tipo de evento desconocido en el outbox: " + tipo);
        }
        return json.readValue(contenido, clase);
    }
}
