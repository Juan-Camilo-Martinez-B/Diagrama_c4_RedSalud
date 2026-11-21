package com.redsaludandina.insights.infrastructure.web;

import com.redsaludandina.insights.application.query.AccessLogLine;
import com.redsaludandina.insights.application.query.GetUsageIndicatorQuery;
import com.redsaludandina.insights.application.query.UsageIndicatorView;
import com.redsaludandina.insights.application.query.WhoAccessedQuery;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lado de lectura de Analítica y evidencia. Este contexto no tiene comandos en la API: su lado de
 * escritura son los consumidores de eventos.
 */
@RestController
public class InsightsQueryController {

    private final QueryHandler<WhoAccessedQuery, List<AccessLogLine>> whoAccessed;
    private final QueryHandler<GetUsageIndicatorQuery, UsageIndicatorView> getUsageIndicator;

    public InsightsQueryController(QueryHandler<WhoAccessedQuery, List<AccessLogLine>> whoAccessed,
                                   QueryHandler<GetUsageIndicatorQuery, UsageIndicatorView> getUsageIndicator) {
        this.whoAccessed = whoAccessed;
        this.getUsageIndicator = getUsageIndicator;
    }

    @GetMapping("/insights/access-log")
    List<AccessLogLine> accessLog(@RequestParam String resource) {
        return whoAccessed.handle(new WhoAccessedQuery(resource));
    }

    @GetMapping("/insights/indicators/{centerId}/{period}")
    UsageIndicatorView indicator(@PathVariable String centerId, @PathVariable String period) {
        return getUsageIndicator.handle(new GetUsageIndicatorQuery(centerId, period));
    }
}
