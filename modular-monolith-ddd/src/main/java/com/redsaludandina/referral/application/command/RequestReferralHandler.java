package com.redsaludandina.referral.application.command;

import com.redsaludandina.referral.application.port.ReferralRepository;
import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.referral.domain.model.Referral.ReferralData;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

/** Pedir dos veces la misma interconsulta (mismo ULID) no la duplica. */
public final class RequestReferralHandler implements CommandHandler<RequestReferralCommand> {

    private final ReferralRepository referrals;
    private final ReferralWriter writer;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RequestReferralHandler(ReferralRepository referrals, ReferralWriter writer, UnitOfWork unitOfWork,
                                  Clock clock) {
        this.referrals = Require.notNull(referrals, "referrals");
        this.writer = Require.notNull(writer, "writer");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(RequestReferralCommand command) {
        Referral referral = Referral.request(new ReferralData(command.referralId(), command.originEpisodeId(),
                command.patientId(), command.centerId(), command.specialty(), command.reason(), clock.instant()));
        unitOfWork.run(() -> {
            if (!referrals.exists(referral.id())) {
                writer.save(referral);
            }
        });
    }
}
