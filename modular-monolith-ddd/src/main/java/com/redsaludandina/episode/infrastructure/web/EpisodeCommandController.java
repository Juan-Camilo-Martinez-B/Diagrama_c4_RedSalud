package com.redsaludandina.episode.infrastructure.web;

import com.redsaludandina.episode.application.command.AttachFileCommand;
import com.redsaludandina.episode.application.command.CloseTeleconsultationCommand;
import com.redsaludandina.episode.application.command.OpenEpisodeCommand;
import com.redsaludandina.episode.application.command.RecordCareCommand;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.domain.ClinicalId;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lado de escritura del episodio en línea: cada endpoint despacha un comando y responde 204. */
@RestController
@RequestMapping("/episodes")
public class EpisodeCommandController {

    private final CommandHandler<OpenEpisodeCommand> openEpisode;
    private final CommandHandler<RecordCareCommand> recordCare;
    private final CommandHandler<AttachFileCommand> attachFile;
    private final CommandHandler<CloseTeleconsultationCommand> closeTeleconsultation;

    public EpisodeCommandController(CommandHandler<OpenEpisodeCommand> openEpisode,
                                    CommandHandler<RecordCareCommand> recordCare,
                                    CommandHandler<AttachFileCommand> attachFile,
                                    CommandHandler<CloseTeleconsultationCommand> closeTeleconsultation) {
        this.openEpisode = openEpisode;
        this.recordCare = recordCare;
        this.attachFile = attachFile;
        this.closeTeleconsultation = closeTeleconsultation;
    }

    record OpenEpisodeRequest(String episodeId, String patientId, String slotId, String centerId) {
    }

    record RecordCareRequest(String text, String centerId, Instant stampedAt) {
    }

    record AttachFileRequest(String attachmentId, String type, String storageKey, String sha256) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void open(@RequestBody OpenEpisodeRequest request) {
        openEpisode.handle(new OpenEpisodeCommand(ClinicalId.of(request.episodeId()), request.patientId(),
                ClinicalId.ofNullable(request.slotId()), request.centerId()));
    }

    @PutMapping("/{episodeId}/care/{field}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void recordCare(@PathVariable String episodeId, @PathVariable String field, @RequestBody RecordCareRequest request) {
        recordCare.handle(new RecordCareCommand(ClinicalId.of(episodeId), field, request.text(), request.centerId(),
                request.stampedAt()));
    }

    @PostMapping("/{episodeId}/attachments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void attach(@PathVariable String episodeId, @RequestBody AttachFileRequest request) {
        attachFile.handle(new AttachFileCommand(ClinicalId.of(episodeId), ClinicalId.of(request.attachmentId()),
                request.type(), request.storageKey(), request.sha256()));
    }

    @PostMapping("/{episodeId}/closure")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@PathVariable String episodeId) {
        closeTeleconsultation.handle(new CloseTeleconsultationCommand(ClinicalId.of(episodeId)));
    }
}
