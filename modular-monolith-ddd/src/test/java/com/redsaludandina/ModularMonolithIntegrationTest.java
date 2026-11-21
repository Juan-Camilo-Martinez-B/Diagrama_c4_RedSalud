package com.redsaludandina;

import static com.redsaludandina.TestIds.ulid;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.redsaludandina.shared.infrastructure.messaging.ScheduledOutboxRelay;
import com.redsaludandina.shared.infrastructure.web.Headers;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * El monolito completo sobre H2: comandos y consultas por HTTP, outbox relevado a mano y eventos
 * entre contextos por el bus en proceso.
 */
@SpringBootTest
@ActiveProfiles("test")
class ModularMonolithIntegrationTest {

    private static final String CENTER = "centro-a";
    private static final String PATIENT = "pac-1";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ScheduledOutboxRelay relay;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    /** Releva hasta que no quede nada: un evento puede producir otros (por ejemplo, ReferralClosed). */
    private void relayUntilQuiet() {
        while (relay.relayAll() > 0) {
            Thread.onSpinWait();
        }
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions read(String url, String userId) throws Exception {
        return mvc.perform(get(url).header(Headers.USER, userId));
    }

    @Test
    void eventsMoveTheReferralAndEveryReadIsAudited() throws Exception {
        String referral = ulid(101);
        String slot = ulid(102);
        String episode = ulid(103);
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        send(post("/referrals"), """
                {"referralId":"%s","originEpisodeId":"%s","patientId":"%s","centerId":"%s",
                 "specialty":"Cardiología","reason":"Dolor torácico"}""".formatted(referral, referral, PATIENT, CENTER))
                .andExpect(status().isNoContent());
        send(post("/scheduling/slots"), """
                {"slotId":"%s","specialistId":"esp-1","start":"%s","end":"%s"}"""
                .formatted(slot, start, start.plus(30, ChronoUnit.MINUTES))).andExpect(status().isNoContent());
        send(post("/scheduling/slots/" + slot + "/bookings"), """
                {"bookingId":"%s","patientId":"%s","centerId":"%s"}""".formatted(ulid(104), PATIENT, CENTER))
                .andExpect(status().isNoContent());
        relayUntilQuiet();
        read("/referrals/alerts?centerId=" + CENTER, "med-1")
                .andExpect(jsonPath("$[?(@.referralId=='%s')].status".formatted(referral)).value(hasItem("SCHEDULED")));

        send(post("/episodes"), """
                {"episodeId":"%s","patientId":"%s","slotId":"%s","centerId":"%s"}"""
                .formatted(episode, PATIENT, slot, CENTER)).andExpect(status().isNoContent());
        send(put("/episodes/" + episode + "/care/REASON"), """
                {"text":"Dolor torácico","centerId":"%s","stampedAt":"%s"}""".formatted(CENTER, Instant.now()))
                .andExpect(status().isNoContent());
        mvc.perform(post("/episodes/" + episode + "/closure")).andExpect(status().isNoContent());
        relayUntilQuiet();
        read("/referrals/alerts?centerId=" + CENTER, "med-1")
                .andExpect(jsonPath("$[?(@.referralId=='%s')].status".formatted(referral)).value(hasItem("ATTENDED")));

        read("/episodes/" + episode + "?reason=Control", "med-1")
                .andExpect(status().isOk()).andExpect(jsonPath("$.care.REASON").value("Dolor torácico"));
        read("/episodes/" + episode + "?reason=Curiosidad", "no-profile").andExpect(status().isForbidden());
        relayUntilQuiet();
        read("/insights/access-log?resource=episode/" + episode, "auditor")
                .andExpect(jsonPath("$[*].userId").value(hasItem("med-1")))
                .andExpect(jsonPath("$[*].userId").value(not(hasItem("no-profile"))));
    }

    @Test
    void offlineSyncIsIdempotentPerBatch() throws Exception {
        String episode = ulid(201);
        String batch = """
                {"centerId":"%s","items":[
                  {"changeId":"c1","aggregateId":"%s","sequence":1,"type":"OPEN_EPISODE","episodeId":"%s","patientId":"%s"},
                  {"changeId":"c2","aggregateId":"%s","sequence":2,"type":"RECORD_CARE","episodeId":"%s",
                   "field":"FINDINGS","text":"38,5","stampedAt":"2026-03-01T09:00:00Z"},
                  {"changeId":"c3","aggregateId":"%s","sequence":3,"type":"RECORD_CARE","episodeId":"%s",
                   "field":"FINDINGS","text":"37,9","stampedAt":"2026-03-01T08:00:00Z"},
                  {"changeId":"c4","aggregateId":"%s","sequence":1,"type":"SIGN_PRESCRIPTION","prescriptionId":"%s"}]}"""
                .formatted(CENTER, episode, episode, PATIENT, episode, episode, episode, episode, ulid(202), ulid(202));

        send(post("/episodes/sync").header(Headers.IDEMPOTENCY_KEY, "batch-201"), batch)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[3].code").value("REQUIRES_SERVER"));
        send(post("/episodes/sync").header(Headers.IDEMPOTENCY_KEY, "batch-201"), batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));
        send(post("/episodes/sync").header(Headers.IDEMPOTENCY_KEY, "batch-201"), "{\"centerId\":\"centro-a\",\"items\":[]}")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        read("/episodes/" + episode + "?reason=Revisar", "med-1").andExpect(jsonPath("$.care.FINDINGS").value("38,5"));
    }

    @Test
    void signedPrescriptionsReachThePharmacy() throws Exception {
        String prescription = ulid(301);
        mvc.perform(post("/prescriptions").header(Headers.USER, "med-1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"prescriptionId\":\"%s\",\"episodeId\":\"%s\"}".formatted(prescription, ulid(302))))
                .andExpect(status().isNoContent());
        send(post("/prescriptions/" + prescription + "/medications"),
                "{\"name\":\"Paracetamol\",\"dose\":\"500 mg\",\"instructions\":\"Cada 8 horas\"}")
                .andExpect(status().isNoContent());
        mvc.perform(post("/prescriptions/" + prescription + "/signature").header(Headers.USER, "med-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"signatureValue\":\"firma\"}"))
                .andExpect(status().isNoContent());
        relayUntilQuiet();

        read("/prescriptions/" + prescription + "/status", "med-1")
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.sentToPharmacyAt").value(notNullValue()));
    }

    @Test
    void theLegacyHistoryIsTranslatedCachedAndAudited() throws Exception {
        read("/legacy/history/" + PATIENT + "?centerId=" + CENTER + "&reason=Antecedentes", "med-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.episodes.length()").value(2))
                .andExpect(jsonPath("$.episodes[*].description").value(hasItem("(sin glosa)")))
                .andExpect(jsonPath("$.fresh").value(true));
        read("/legacy/history/" + PATIENT + "?centerId=" + CENTER + "&reason=Antecedentes", "no-profile")
                .andExpect(status().isForbidden());
        relayUntilQuiet();

        read("/insights/access-log?resource=history/" + PATIENT, "auditor")
                .andExpect(jsonPath("$[*].userId").value(hasItem("med-1")));
    }

    @Test
    void usageIndicatorsAreProjectedFromEvents() throws Exception {
        String referral = ulid(401);
        send(post("/referrals"), """
                {"referralId":"%s","originEpisodeId":"%s","patientId":"pac-9","centerId":"centro-z",
                 "specialty":"Pediatría","reason":"Control"}""".formatted(referral, referral))
                .andExpect(status().isNoContent());
        relayUntilQuiet();

        read("/insights/indicators/centro-z/" + YearMonth.now(ZoneOffset.UTC), "auditor")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referralsRequested").value(1));
    }
}
