package com.redsaludandina.scheduling.infrastructure.web;

import com.redsaludandina.scheduling.application.query.AvailableSlotView;
import com.redsaludandina.scheduling.application.query.ListAvailableSlotsQuery;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lado de lectura de Agenda: solo GET, sin efectos. */
@RestController
public class SchedulingQueryController {

    private final QueryHandler<ListAvailableSlotsQuery, List<AvailableSlotView>> listAvailableSlots;

    public SchedulingQueryController(QueryHandler<ListAvailableSlotsQuery, List<AvailableSlotView>> listAvailableSlots) {
        this.listAvailableSlots = listAvailableSlots;
    }

    @GetMapping("/scheduling/slots")
    List<AvailableSlotView> available(@RequestParam String specialistId) {
        return listAvailableSlots.handle(new ListAvailableSlotsQuery(specialistId));
    }
}
