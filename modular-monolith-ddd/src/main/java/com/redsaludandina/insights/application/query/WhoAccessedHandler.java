package com.redsaludandina.insights.application.query;

import com.redsaludandina.insights.application.port.AccessLogReader;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.Require;
import java.util.List;

public final class WhoAccessedHandler implements QueryHandler<WhoAccessedQuery, List<AccessLogLine>> {

    private final AccessLogReader accessLog;

    public WhoAccessedHandler(AccessLogReader accessLog) {
        this.accessLog = Require.notNull(accessLog, "accessLog");
    }

    @Override
    public List<AccessLogLine> handle(WhoAccessedQuery query) {
        return accessLog.clinicalReadsOf(Require.text(query.resource(), "resource"));
    }
}
