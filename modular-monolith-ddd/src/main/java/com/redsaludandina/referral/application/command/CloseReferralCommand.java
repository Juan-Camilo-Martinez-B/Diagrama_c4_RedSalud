package com.redsaludandina.referral.application.command;

import com.redsaludandina.shared.application.cqrs.Command;
import com.redsaludandina.shared.domain.ClinicalId;

/** El especialista cierra la interconsulta ya atendida. */
public record CloseReferralCommand(ClinicalId referralId) implements Command {
}
