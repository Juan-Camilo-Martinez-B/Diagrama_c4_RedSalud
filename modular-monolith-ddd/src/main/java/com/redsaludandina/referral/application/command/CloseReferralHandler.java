package com.redsaludandina.referral.application.command;

import com.redsaludandina.referral.application.port.ReferralRepository;
import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.shared.application.ResourceNotFoundException;
import com.redsaludandina.shared.application.UnitOfWork;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.Require;
import java.time.Clock;

public final class CloseReferralHandler implements CommandHandler<CloseReferralCommand> {

    private final ReferralRepository referrals;
    private final ReferralWriter writer;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CloseReferralHandler(ReferralRepository referrals, ReferralWriter writer, UnitOfWork unitOfWork,
                                Clock clock) {
        this.referrals = Require.notNull(referrals, "referrals");
        this.writer = Require.notNull(writer, "writer");
        this.unitOfWork = Require.notNull(unitOfWork, "unitOfWork");
        this.clock = Require.notNull(clock, "clock");
    }

    @Override
    public void handle(CloseReferralCommand command) {
        Require.notNull(command.referralId(), "referralId");
        unitOfWork.run(() -> {
            Referral referral = referrals.findById(command.referralId())
                    .orElseThrow(() -> new ResourceNotFoundException("la interconsulta " + command.referralId()));
            referral.close(clock.instant());
            writer.save(referral);
        });
    }
}
