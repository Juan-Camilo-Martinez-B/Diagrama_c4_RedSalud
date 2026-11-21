package com.redsaludandina.referral.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** El médico rural pide una interconsulta a una especialidad desde un episodio. */
public record RequestReferralCommand(ClinicalId referralId, ClinicalId originEpisodeId, String patientId,
                                     String centerId, String specialty, String reason) implements Command {
}
