package com.redsaludandina.shared.infrastructure.messaging;

import com.redsaludandina.shared.domain.DomainEvent;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/** Convierte los eventos del lenguaje publicado a JSON y de vuelta, por su nombre de tipo. */
public final class EventSerializer {

    private final JsonMapper json;
    private final Map<String, Class<? extends DomainEvent>> types;

    public EventSerializer(JsonMapper json, Collection<Class<? extends DomainEvent>> types) {
        this.json = json;
        this.types = types.stream().collect(Collectors.toUnmodifiableMap(Class::getSimpleName, Function.identity()));
    }

    public String type(DomainEvent event) {
        String type = event.getClass().getSimpleName();
        if (!types.containsKey(type)) {
            throw new IllegalArgumentException("Evento no registrado en el lenguaje publicado: " + type);
        }
        return type;
    }

    public String toJson(DomainEvent event) {
        type(event);
        return json.writeValueAsString(event);
    }

    public DomainEvent fromJson(String type, String content) {
        Class<? extends DomainEvent> eventClass = types.get(type);
        if (eventClass == null) {
            throw new IllegalStateException("Tipo de evento desconocido: " + type);
        }
        return json.readValue(content, eventClass);
    }
}
