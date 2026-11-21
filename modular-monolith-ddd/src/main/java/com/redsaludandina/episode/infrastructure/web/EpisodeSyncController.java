package com.redsaludandina.episode.infrastructure.web;

import com.redsaludandina.episode.application.command.sync.SyncOfflineBatchCommand;
import com.redsaludandina.episode.application.query.GetSyncResultQuery;
import com.redsaludandina.episode.application.query.SyncResultView;
import com.redsaludandina.episode.domain.sync.IdempotencyKeyReusedException;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.infrastructure.web.ErrorHandler;
import com.redsaludandina.shared.infrastructure.web.Headers;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sincronización offline de episodios (diagrama de secuencia del caso). Despacha el comando del lote
 * y luego consulta la respuesta R: 201 si el lote se procesó, 200 si se repitió R, 422 si la
 * Idempotency-Key ya se usó con otro lote.
 */
@RestController
public class EpisodeSyncController {

    private final CommandHandler<SyncOfflineBatchCommand> syncBatch;
    private final QueryHandler<GetSyncResultQuery, SyncResultView> getSyncResult;

    public EpisodeSyncController(CommandHandler<SyncOfflineBatchCommand> syncBatch,
                                 QueryHandler<GetSyncResultQuery, SyncResultView> getSyncResult) {
        this.syncBatch = syncBatch;
        this.getSyncResult = getSyncResult;
    }

    record SyncBatchRequest(String centerId, List<SyncItemRequest> items) {
    }

    @PostMapping("/episodes/sync")
    ResponseEntity<SyncResultView> sync(@RequestHeader(Headers.IDEMPOTENCY_KEY) String idempotencyKey,
                                        @RequestBody SyncBatchRequest request) {
        List<SyncItemRequest> items = Optional.ofNullable(request.items()).orElse(List.of());
        syncBatch.handle(new SyncOfflineBatchCommand(idempotencyKey, request.centerId(),
                items.stream().map(SyncItemRequest::toItem).toList()));
        SyncResultView result = getSyncResult.handle(new GetSyncResultQuery(idempotencyKey.strip()));
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    ProblemDetail keyReused(IdempotencyKeyReusedException error) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, error.getMessage());
        problem.setProperty(ErrorHandler.CODE, error.code());
        return problem;
    }
}
