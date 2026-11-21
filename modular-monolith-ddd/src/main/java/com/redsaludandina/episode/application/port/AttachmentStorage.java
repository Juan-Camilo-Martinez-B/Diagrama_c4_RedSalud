package com.redsaludandina.episode.application.port;

import com.redsaludandina.episode.application.query.DownloadLink;
import com.redsaludandina.episode.application.query.UploadLink;
import com.redsaludandina.shared.domain.ClinicalId;

/**
 * Almacén de adjuntos (S3 con KMS, puerto de salida). El servidor decide la clave de cada adjunto y
 * entrega URL prefirmadas: el archivo viaja directo entre el dispositivo y S3.
 */
public interface AttachmentStorage {

    String keyFor(ClinicalId episodeId, ClinicalId attachmentId);

    UploadLink presignUpload(ClinicalId episodeId, ClinicalId attachmentId, String sha256);

    DownloadLink presignDownload(String storageKey);
}
