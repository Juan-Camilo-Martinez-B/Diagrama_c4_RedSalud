package com.redsaludandina.referral.application.port;

import com.redsaludandina.referral.application.query.PendingReferralView;
import java.util.List;

/** Lado de lectura de Interconsulta (puerto de salida). */
public interface ReferralReadModel {

    List<PendingReferralView> withPendingAlerts(String centerId);
}
