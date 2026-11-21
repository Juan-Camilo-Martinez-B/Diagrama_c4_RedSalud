package com.redsaludandina.insights.application.event;

import com.redsaludandina.insights.application.event.IndicatorProjection.Rule;
import com.redsaludandina.insights.domain.analytics.UsageIndicator;
import com.redsaludandina.shared.event.PatientAttended;
import com.redsaludandina.shared.event.ReferralClosed;
import com.redsaludandina.shared.event.ReferralRequested;
import com.redsaludandina.shared.event.SlotBooked;

/** Qué evento alimenta qué indicador. Agregar un indicador es agregar una regla (abierto/cerrado). */
public final class InsightsRules {

    public static final Rule<SlotBooked> BOOKINGS = new Rule<>("insights.indicators.slot-booked",
            SlotBooked.class, SlotBooked::centerId, UsageIndicator::onSlotBooked);

    public static final Rule<PatientAttended> TELECONSULTATIONS = new Rule<>("insights.indicators.patient-attended",
            PatientAttended.class, PatientAttended::centerId, UsageIndicator::onPatientAttended);

    public static final Rule<ReferralRequested> REFERRALS_REQUESTED = new Rule<>(
            "insights.indicators.referral-requested", ReferralRequested.class, ReferralRequested::centerId,
            UsageIndicator::onReferralRequested);

    public static final Rule<ReferralClosed> REFERRALS_CLOSED = new Rule<>("insights.indicators.referral-closed",
            ReferralClosed.class, ReferralClosed::centerId, UsageIndicator::onReferralClosed);

    private InsightsRules() {
    }
}
