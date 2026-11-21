package com.redsaludandina.referral.application.query;

import com.redsaludandina.shared.application.cqrs.Query;
import java.util.List;

/** ¿Qué interconsultas del centro esperan un paso (agendar, atender o cerrar)? */
public record ListPendingAlertsQuery(String centerId) implements Query<List<PendingReferralView>> {
}
