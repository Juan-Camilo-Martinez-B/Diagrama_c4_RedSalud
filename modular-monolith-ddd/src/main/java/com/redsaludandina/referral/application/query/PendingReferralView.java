package com.redsaludandina.referral.application.query;

import java.util.List;

/** Modelo de lectura: una interconsulta del centro con sus alertas pendientes. */
public record PendingReferralView(String referralId, String specialty, String status, List<String> alerts) {
}
