package com.redsaludandina.identity.application.command;

import com.redsaludandina.identity.application.port.UserRepository;
import com.redsaludandina.identity.domain.model.AccessDecision;
import com.redsaludandina.identity.domain.model.ClinicalResource;
import com.redsaludandina.identity.domain.service.AccessPolicy;
import com.redsaludandina.shared.application.ForbiddenAccessException;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.domain.Require;
import com.redsaludandina.shared.event.AccessDenied;
import java.time.Clock;
import java.util.List;

/** Evalúa la política de acceso; si niega, registra {@link AccessDenied} en el outbox y falla. */
public final class AuthorizeAccessHandler implements CommandHandler<AuthorizeAccessCommand> {

    private final UserRepository users;
    private final AccessPolicy policy;
    private final UnitOfWork unitOfWork;
    private final OutboxRecorder outbox;
    private final Clock clock;

    public AuthorizeAccessHandler(UserRepository users, AccessPolicy policy, UnitOfWork unitOfWork,
                                  OutboxRecorder outbox, Clock clock) {
        this.users = Require.notNull(users, "users");
        this.policy = Require.notNull(policy, "policy");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.outbox = Require.notNull(outbox, "outbox");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(AuthorizeAccessCommand command) {
        Require.notNull(command, "command");
        AccessDecision decision = unitOfWork.call(() -> decide(command));
        if (!decision.allowed()) {
            throw new ForbiddenAccessException(decision.reason());
        }
    }

    private AccessDecision decide(AuthorizeAccessCommand command) {
        ClinicalResource resource = new ClinicalResource(command.resource(), command.patientId(), command.centerId(),
                command.specialty());
        String userId = Require.text(command.userId(), "userId");
        AccessDecision decision = policy.evaluate(users.findById(userId), resource);
        if (!decision.allowed()) {
            outbox.record(userId, List.of(new AccessDenied(userId, resource.reference(), decision.reason(),
                    clock.instant())));
        }
        return decision;
    }
}
