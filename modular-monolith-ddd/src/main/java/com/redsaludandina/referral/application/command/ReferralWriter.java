package com.redsaludandina.referral.application.command;

import com.redsaludandina.referral.application.port.ReferralRepository;
import com.redsaludandina.referral.domain.model.Referral;
import com.redsaludandina.shared.application.messaging.OutboxRecorder;
import com.redsaludandina.shared.domain.Require;

/** Guarda la interconsulta y sus eventos juntos; se usa siempre dentro de una unidad de trabajo. */
public final class ReferralWriter {

    private final ReferralRepository referrals;
    private final OutboxRecorder outbox;

    public ReferralWriter(ReferralRepository referrals, OutboxRecorder outbox) {
        this.referrals = Require.notNull(referrals, "referrals");
        this.outbox = Require.notNull(outbox, "outbox");
    }

    public void save(Referral referral) {
        referrals.save(referral);
        outbox.record(referral);
    }
}
