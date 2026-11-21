package com.redsaludandina.episode.domain.model;

import com.redsaludandina.shared.domain.Require;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Nota de atención del episodio (objeto valor inmutable): un valor clínico por campo. */
public final class CareRecord {

    private final Map<CareField, ClinicalValue> values;

    private CareRecord(Map<CareField, ClinicalValue> values) {
        EnumMap<CareField, ClinicalValue> copy = new EnumMap<>(CareField.class);
        copy.putAll(values);
        this.values = Collections.unmodifiableMap(copy);
    }

    public static CareRecord empty() {
        return new CareRecord(Map.of());
    }

    public static CareRecord of(Map<CareField, ClinicalValue> values) {
        return new CareRecord(Require.notNull(values, "values"));
    }

    public Optional<ClinicalValue> value(CareField field) {
        return Optional.ofNullable(values.get(field));
    }

    public boolean has(CareField field) {
        return values.containsKey(field);
    }

    public CareRecord with(CareField field, ClinicalValue value) {
        EnumMap<CareField, ClinicalValue> updated = new EnumMap<>(CareField.class);
        updated.putAll(values);
        updated.put(Require.notNull(field, "field"), Require.notNull(value, "value"));
        return new CareRecord(updated);
    }

    public Map<CareField, ClinicalValue> values() {
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CareRecord careRecord && values.equals(careRecord.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }
}
