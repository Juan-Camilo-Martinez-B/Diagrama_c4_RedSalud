package com.redsaludandina.referral.infrastructure.web;

import com.redsaludandina.referral.application.command.CloseReferralCommand;
import com.redsaludandina.referral.application.command.RequestReferralCommand;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.ClinicalId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado de escritura de Interconsulta. Agendar y atender ocurren por eventos, no por la API. */
@RestController
@RequestMapping("/referrals")
public class ReferralCommandController {

    private final CommandHandler<RequestReferralCommand> requestReferral;
    private final CommandHandler<CloseReferralCommand> closeReferral;

    public ReferralCommandController(CommandHandler<RequestReferralCommand> requestReferral,
                                     CommandHandler<CloseReferralCommand> closeReferral) {
        this.requestReferral = requestReferral;
        this.closeReferral = closeReferral;
    }

    record RequestReferralRequest(String referralId, String originEpisodeId, String patientId, String centerId,
                                  String specialty, String reason) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void request(@RequestBody RequestReferralRequest request) {
        requestReferral.handle(new RequestReferralCommand(ClinicalId.of(request.referralId()),
                ClinicalId.of(request.originEpisodeId()), request.patientId(), request.centerId(),
                request.specialty(), request.reason()));
    }

    @PostMapping("/{referralId}/closure")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@PathVariable String referralId) {
        closeReferral.handle(new CloseReferralCommand(ClinicalId.of(referralId)));
    }
}
