package com.redsaludandina.insights.application.query;

import com.redsaludandina.shared.application.cqrs.Query;
import java.util.List;

/** ¿Quién vio este recurso clínico? (Caso §5: responder con evidencia inmutable). */
public record WhoAccessedQuery(String resource) implements Query<List<AccessLogLine>> {
}
