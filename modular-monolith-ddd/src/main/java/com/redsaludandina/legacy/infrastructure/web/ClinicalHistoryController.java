package com.redsaludandina.legacy.infrastructure.web;

import com.redsaludandina.legacy.application.command.RecordHistoryReadCommand;
import com.redsaludandina.legacy.application.command.RefreshClinicalHistoryCommand;
import com.redsaludandina.legacy.application.port.LegacyUnavailableException;
import com.redsaludandina.legacy.application.query.ClinicalHistoryView;
import com.redsaludandina.legacy.application.query.GetClinicalHistoryQuery;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.infrastructure.web.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historial clínico del legado. Orden CQRS: (1) autorizar y auditar la lectura, (2) refrescar el
 * modelo de lectura si caducó, (3) consultar el modelo de lectura sin efectos.
 */
@RestController
public class ClinicalHistoryController {

    private final CommandHandler<RecordHistoryReadCommand> recordRead;
    private final CommandHandler<RefreshClinicalHistoryCommand> refresh;
    private final QueryHandler<GetClinicalHistoryQuery, ClinicalHistoryView> getHistory;

    public ClinicalHistoryController(CommandHandler<RecordHistoryReadCommand> recordRead,
                                     CommandHandler<RefreshClinicalHistoryCommand> refresh,
                                     QueryHandler<GetClinicalHistoryQuery, ClinicalHistoryView> getHistory) {
        this.recordRead = recordRead;
        this.refresh = refresh;
        this.getHistory = getHistory;
    }

    @GetMapping("/legacy/history/{patientId}")
    ClinicalHistoryView history(@PathVariable String patientId, @RequestParam String centerId,
                                @RequestParam String reason, @AuthenticatedUser String userId) {
        recordRead.handle(new RecordHistoryReadCommand(userId, patientId, centerId, reason));
        refresh.handle(new RefreshClinicalHistoryCommand(patientId));
        return getHistory.handle(new GetClinicalHistoryQuery(patientId));
    }

    @ExceptionHandler(LegacyUnavailableException.class)
    ProblemDetail legacyUnavailable(LegacyUnavailableException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, error.getMessage());
    }
}
