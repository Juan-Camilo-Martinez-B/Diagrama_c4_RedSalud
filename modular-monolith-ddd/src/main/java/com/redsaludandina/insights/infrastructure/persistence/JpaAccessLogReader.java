package com.redsaludandina.insights.infrastructure.persistence;

import com.redsaludandina.insights.application.port.AccessLogReader;
import com.redsaludandina.insights.application.query.AccessLogLine;
import java.util.List;

public final class JpaAccessLogReader implements AccessLogReader {

    private final AccessRecordQueryRepository queries;

    public JpaAccessLogReader(AccessRecordQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public List<AccessLogLine> clinicalReadsOf(String resource) {
        return queries.findClinicalReads(resource);
    }
}
