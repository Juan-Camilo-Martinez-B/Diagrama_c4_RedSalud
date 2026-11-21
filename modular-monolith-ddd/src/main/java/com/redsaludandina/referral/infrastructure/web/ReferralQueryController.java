package com.redsaludandina.referral.infrastructure.web;

import com.redsaludandina.referral.application.query.ListPendingAlertsQuery;
import com.redsaludandina.referral.application.query.PendingReferralView;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lado de lectura de Interconsulta: las alertas pendientes del centro. */
@RestController
public class ReferralQueryController {

    private final QueryHandler<ListPendingAlertsQuery, List<PendingReferralView>> listPendingAlerts;

    public ReferralQueryController(QueryHandler<ListPendingAlertsQuery, List<PendingReferralView>> listPendingAlerts) {
        this.listPendingAlerts = listPendingAlerts;
    }

    @GetMapping("/referrals/alerts")
    List<PendingReferralView> alerts(@RequestParam String centerId) {
        return listPendingAlerts.handle(new ListPendingAlertsQuery(centerId));
    }
}
