package com.redsaludandina.receta.infraestructura.adaptador.salida.farmacia;

import com.redsaludandina.receta.aplicacion.puerto.salida.FarmaciaPort;
import com.redsaludandina.receta.dominio.modelo.FirmaDigital;
import com.redsaludandina.receta.dominio.modelo.Receta;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envío de la receta firmada a la farmacia del centro por una cola SQS FIFO (consistencia fuerte
 * local, eventual hacia la farmacia, Caso §2). El id de la receta es grupo y clave de
 * deduplicación: un reenvío dentro de la ventana de SQS se descarta, y fuera de ella la farmacia
 * debe descartarlo por el mismo id.
 */
public final class FarmaciaSqs implements FarmaciaPort {

    /** Mensaje que recibe la farmacia. */
    record RecetaParaFarmacia(String recetaId, String episodioId, String prescriptorId, String firmanteId,
                              Instant firmadaEn, List<LineaReceta> medicamentos) {
    }

    record LineaReceta(String nombre, String dosis, String indicacion) {
    }

    private final SqsClient sqs;
    private final String colaUrl;
    private final JsonMapper json;

    public FarmaciaSqs(SqsClient sqs, String colaUrl, JsonMapper json) {
        if (colaUrl == null || !colaUrl.endsWith(".fifo")) {
            throw new IllegalArgumentException("La cola de farmacia debe ser SQS FIFO (.fifo): " + colaUrl);
        }
        this.sqs = sqs;
        this.colaUrl = colaUrl;
        this.json = json;
    }

    @Override
    public void enviar(Receta receta) {
        FirmaDigital firma = receta.firma()
                .orElseThrow(() -> new IllegalStateException("Solo se envían recetas firmadas: " + receta.id()));
        RecetaParaFarmacia mensaje = new RecetaParaFarmacia(receta.id().valor(), receta.episodioId().valor(),
                receta.prescriptorId(), firma.firmanteId(), firma.firmadaEn(),
                receta.medicamentos().stream()
                        .map(medicamento -> new LineaReceta(medicamento.nombre(), medicamento.dosis(), medicamento.indicacion()))
                        .toList());
        sqs.sendMessage(envio -> envio
                .queueUrl(colaUrl)
                .messageGroupId(receta.id().valor())
                .messageDeduplicationId(receta.id().valor())
                .messageBody(json.writeValueAsString(mensaje)));
    }
}
