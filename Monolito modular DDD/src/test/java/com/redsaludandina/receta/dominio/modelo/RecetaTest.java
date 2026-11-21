package com.redsaludandina.receta.dominio.modelo;

import static com.redsaludandina.compartido.dominio.IdsDePrueba.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.evento.RecetaFirmada;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecetaTest {

    private static final Instant T = Instant.parse("2026-11-20T16:00:00Z");
    private static final Medicamento AMOXICILINA = new Medicamento("Amoxicilina", "500 mg", "Cada 8 horas por 7 días");

    private Receta receta() {
        return Receta.redactar(id(1), id(2), "esp-1");
    }

    private static FirmaDigital firmaDe(String firmante) {
        return new FirmaDigital(firmante, T, "firma-" + firmante);
    }

    private static void esperarCodigo(Runnable accion, String codigo) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ReglaDeNegocioException.class,
                error -> assertThat(error.codigo()).isEqualTo(codigo));
    }

    @Test
    void firmarPublicaRecetaFirmada() {
        Receta receta = receta();
        receta.agregarMedicamento(AMOXICILINA);

        receta.firmar(firmaDe("esp-1"));

        assertThat(receta.estado()).isEqualTo(EstadoReceta.FIRMADA);
        assertThat(receta.extraerEventos()).singleElement()
                .isInstanceOfSatisfying(RecetaFirmada.class, evento -> assertThat(evento.episodioId()).isEqualTo(id(2)));
    }

    @Test
    void unaRecetaFirmadaEsInmutable() {
        Receta receta = receta();
        receta.agregarMedicamento(AMOXICILINA);
        receta.firmar(firmaDe("esp-1"));

        esperarCodigo(() -> receta.agregarMedicamento(AMOXICILINA), Receta.RECETA_FIRMADA_INMUTABLE);
        esperarCodigo(() -> receta.firmar(new FirmaDigital("esp-1", T.plusSeconds(5), "otra")),
                Receta.RECETA_FIRMADA_INMUTABLE);
        assertThat(receta.medicamentos()).hasSize(1);
    }

    @Test
    void reenviarLaMismaFirmaEsIdempotente() {
        Receta receta = receta();
        receta.agregarMedicamento(AMOXICILINA);
        receta.firmar(firmaDe("esp-1"));
        receta.extraerEventos();

        receta.firmar(firmaDe("esp-1"));

        assertThat(receta.extraerEventos()).isEmpty();
    }

    @Test
    void noSeFirmaSinMedicamentosNiPorQuienNoLaPrescribio() {
        Receta vacia = receta();
        esperarCodigo(() -> vacia.firmar(firmaDe("esp-1")), Receta.RECETA_SIN_MEDICAMENTOS);

        Receta ajena = receta();
        ajena.agregarMedicamento(AMOXICILINA);
        esperarCodigo(() -> ajena.firmar(firmaDe("esp-2")), Receta.FIRMANTE_NO_ES_PRESCRIPTOR);
    }

    @Test
    void soloUnaRecetaFirmadaSeEnviaALaFarmacia() {
        Receta receta = receta();
        receta.agregarMedicamento(AMOXICILINA);
        esperarCodigo(() -> receta.marcarEnviadaAFarmacia(T), Receta.RECETA_NO_FIRMADA);

        receta.firmar(firmaDe("esp-1"));
        receta.marcarEnviadaAFarmacia(T);
        receta.marcarEnviadaAFarmacia(T.plusSeconds(60));

        assertThat(receta.enviadaAFarmaciaEn()).contains(T);
    }
}
