package com.redsaludandina.episode.infrastructure.web;

import com.redsaludandina.episode.application.command.AuthorizeEpisodeAccessCommand;
import com.redsaludandina.episode.application.command.RecordEpisodeReadCommand;
import com.redsaludandina.episode.application.query.DownloadLink;
import com.redsaludandina.episode.application.query.EpisodeView;
import com.redsaludandina.episode.application.query.GetAttachmentDownloadLinkQuery;
import com.redsaludandina.episode.application.query.GetAttachmentUploadLinkQuery;
import com.redsaludandina.episode.application.query.GetEpisodeQuery;
import com.redsaludandina.episode.application.query.UploadLink;
import com.redsaludandina.shared.application.cqrs.CommandHandler;
import com.redsaludandina.shared.application.cqrs.QueryHandler;
import com.redsaludandina.shared.domain.ClinicalId;
import com.redsaludandina.shared.infrastructure.web.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lecturas del episodio. Cada lectura clínica despacha PRIMERO el comando que autoriza y audita, y
 * DESPUÉS la consulta, que no tiene efectos: así CQRS se cumple y ninguna lectura queda sin auditar.
 */
@RestController
@RequestMapping("/episodes/{episodeId}")
public class EpisodeQueryController {

    private final CommandHandler<RecordEpisodeReadCommand> recordRead;
    private final CommandHandler<AuthorizeEpisodeAccessCommand> authorizeAccess;
    private final QueryHandler<GetEpisodeQuery, EpisodeView> getEpisode;
    private final QueryHandler<GetAttachmentUploadLinkQuery, UploadLink> getUploadLink;
    private final QueryHandler<GetAttachmentDownloadLinkQuery, DownloadLink> getDownloadLink;

    public EpisodeQueryController(CommandHandler<RecordEpisodeReadCommand> recordRead,
                                  CommandHandler<AuthorizeEpisodeAccessCommand> authorizeAccess,
                                  QueryHandler<GetEpisodeQuery, EpisodeView> getEpisode,
                                  QueryHandler<GetAttachmentUploadLinkQuery, UploadLink> getUploadLink,
                                  QueryHandler<GetAttachmentDownloadLinkQuery, DownloadLink> getDownloadLink) {
        this.recordRead = recordRead;
        this.authorizeAccess = authorizeAccess;
        this.getEpisode = getEpisode;
        this.getUploadLink = getUploadLink;
        this.getDownloadLink = getDownloadLink;
    }

    @GetMapping
    EpisodeView episode(@PathVariable String episodeId, @RequestParam String reason, @AuthenticatedUser String userId) {
        recordRead.handle(new RecordEpisodeReadCommand(ClinicalId.of(episodeId), null, userId, reason));
        return getEpisode.handle(new GetEpisodeQuery(episodeId));
    }

    @GetMapping("/attachments/{attachmentId}/upload-link")
    UploadLink uploadLink(@PathVariable String episodeId, @PathVariable String attachmentId,
                          @RequestParam String sha256, @AuthenticatedUser String userId) {
        authorizeAccess.handle(new AuthorizeEpisodeAccessCommand(ClinicalId.of(episodeId), userId));
        return getUploadLink.handle(new GetAttachmentUploadLinkQuery(ClinicalId.of(episodeId),
                ClinicalId.of(attachmentId), sha256));
    }

    @GetMapping("/attachments/{attachmentId}")
    DownloadLink downloadLink(@PathVariable String episodeId, @PathVariable String attachmentId,
                              @RequestParam String reason, @AuthenticatedUser String userId) {
        recordRead.handle(new RecordEpisodeReadCommand(ClinicalId.of(episodeId), ClinicalId.of(attachmentId), userId,
                reason));
        return getDownloadLink.handle(new GetAttachmentDownloadLinkQuery(episodeId, attachmentId));
    }
}
