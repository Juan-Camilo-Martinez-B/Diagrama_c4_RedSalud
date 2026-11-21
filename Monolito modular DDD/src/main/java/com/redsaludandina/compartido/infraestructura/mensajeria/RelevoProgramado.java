package com.redsaludandina.compartido.infraestructura.mensajeria;

import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox;
import com.redsaludandina.compartido.aplicacion.mensajeria.RelevoOutbox.ResultadoRelevo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Corre periódicamente el relevo del outbox de cada contexto. Con
 * {@code redsalud.outbox.relevo-automatico=false} (pruebas) no corre solo y se invoca a mano.
 */
public class RelevoProgramado {

    private static final Logger LOG = LoggerFactory.getLogger(RelevoProgramado.class);

    private final List<RelevoOutbox> relevos;
    private final boolean automatico;
    private final int lote;

    public RelevoProgramado(List<RelevoOutbox> relevos, boolean automatico, int lote) {
        this.relevos = List.copyOf(relevos);
        this.automatico = automatico;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${redsalud.outbox.intervalo:PT2S}")
    public void programado() {
        if (automatico) {
            relevarTodo();
        }
    }

    /** Releva todos los outbox una vez y devuelve cuántos mensajes se publicaron. */
    public int relevarTodo() {
        int publicados = 0;
        for (RelevoOutbox relevo : relevos) {
            ResultadoRelevo resultado = relevo.relevar(lote);
            publicados += resultado.publicados();
            if (resultado.fallidos() > 0) {
                LOG.warn("Relevo con fallas: {}", resultado);
            }
        }
        return publicados;
    }
}
