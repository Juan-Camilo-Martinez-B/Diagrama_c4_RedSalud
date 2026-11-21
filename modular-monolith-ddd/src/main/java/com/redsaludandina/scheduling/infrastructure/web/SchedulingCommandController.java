package com.redsaludandina.scheduling.infrastructure.web;

import com.redsaludandina.scheduling.application.command.BookSlotCommand;
import com.redsaludandina.scheduling.application.command.PublishSlotCommand;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.ClinicalId;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado de escritura de Agenda: cada endpoint despacha un comando y responde 204 sin cuerpo. */
@RestController
@RequestMapping("/scheduling/slots")
public class SchedulingCommandController {

    private final CommandHandler<PublishSlotCommand> publishSlot;
    private final CommandHandler<BookSlotCommand> bookSlot;

    public SchedulingCommandController(CommandHandler<PublishSlotCommand> publishSlot,
                                       CommandHandler<BookSlotCommand> bookSlot) {
        this.publishSlot = publishSlot;
        this.bookSlot = bookSlot;
    }

    record PublishSlotRequest(String slotId, String specialistId, Instant start, Instant end) {
    }

    record BookSlotRequest(String bookingId, String patientId, String centerId) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void publish(@RequestBody PublishSlotRequest request) {
        publishSlot.handle(new PublishSlotCommand(ClinicalId.of(request.slotId()), request.specialistId(),
                request.start(), request.end()));
    }

    @PostMapping("/{slotId}/bookings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void book(@PathVariable String slotId, @RequestBody BookSlotRequest request) {
        bookSlot.handle(new BookSlotCommand(ClinicalId.of(slotId), ClinicalId.of(request.bookingId()),
                request.patientId(), request.centerId()));
    }
}
