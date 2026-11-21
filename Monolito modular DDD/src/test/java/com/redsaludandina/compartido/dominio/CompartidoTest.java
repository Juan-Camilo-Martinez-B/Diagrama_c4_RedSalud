package com.redsaludandina.compartido.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CompartidoTest {

    @Test
    void elIdClinicoAceptaUnUlidYLoNormalizaAMayusculas() {
        assertThat(IdClinico.de("01arz3ndektsv4rrffq69g5fav").valor()).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAV");
    }

    @Test
    void elIdClinicoRechazaLoQueNoEsUnUlid() {
        assertThatThrownBy(() -> IdClinico.de("123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdClinico.de("01ARZ3NDEKTSV4RRFFQ69G5FAU")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IdClinico.de("81ARZ3NDEKTSV4RRFFQ69G5FAV")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elSelloMasRecienteGanaYElEmpateLoDecideElCentro() {
        Instant t = Instant.parse("2026-11-01T10:00:00Z");
        SelloCentro antes = new SelloCentro("centro-a", t);
        SelloCentro despues = new SelloCentro("centro-a", t.plusSeconds(1));
        SelloCentro mismoInstanteOtroCentro = new SelloCentro("centro-b", t);

        assertThat(despues.esPosteriorA(antes)).isTrue();
        assertThat(antes.esPosteriorA(despues)).isFalse();
        assertThat(mismoInstanteOtroCentro.esPosteriorA(antes)).isTrue();
    }
}
