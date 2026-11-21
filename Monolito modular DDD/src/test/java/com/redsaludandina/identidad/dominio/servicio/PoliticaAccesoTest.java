package com.redsaludandina.identidad.dominio.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.identidad.dominio.evento.AccesoDenegado;
import com.redsaludandina.identidad.dominio.modelo.AtributosAcceso;
import com.redsaludandina.identidad.dominio.modelo.DecisionAcceso;
import com.redsaludandina.identidad.dominio.modelo.RecursoClinico;
import com.redsaludandina.identidad.dominio.modelo.Rol;
import com.redsaludandina.identidad.dominio.modelo.Usuario;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PoliticaAccesoTest {

    private final PoliticaAcceso politica = new PoliticaAcceso();
    private final RecursoClinico historiaDePac1 = new RecursoClinico("episodio/1", "pac-1", "centro-a", null);
    private final RecursoClinico cardiologiaDePac1 = new RecursoClinico("interconsulta/1", "pac-1", "centro-a", "Cardiología");

    private static Usuario medicoRural(String centro, Set<String> pacientes) {
        return new Usuario("med-1", Rol.MEDICO_RURAL, new AtributosAcceso(centro, null, pacientes));
    }

    private static Usuario especialista(String especialidad, Set<String> pacientes) {
        return new Usuario("esp-1", Rol.ESPECIALISTA, new AtributosAcceso(null, especialidad, pacientes));
    }

    @Test
    void unPacienteSoloVeSuPropiaHistoria() {
        Usuario paciente = new Usuario("pac-1", Rol.PACIENTE, new AtributosAcceso(null, null, Set.of()));
        Usuario otroPaciente = new Usuario("pac-2", Rol.PACIENTE, new AtributosAcceso(null, null, Set.of()));

        assertThat(politica.evaluar(paciente, historiaDePac1).permitido()).isTrue();
        assertThat(politica.evaluar(otroPaciente, historiaDePac1).permitido()).isFalse();
    }

    @Test
    void elMedicoRuralNecesitaSerDelCentroYTenerRelacionConElPaciente() {
        assertThat(politica.evaluar(medicoRural("centro-a", Set.of("pac-1")), historiaDePac1).permitido()).isTrue();
        assertThat(politica.evaluar(medicoRural("centro-b", Set.of("pac-1")), historiaDePac1).razon())
                .contains("otro centro");
        assertThat(politica.evaluar(medicoRural("centro-a", Set.of("pac-9")), historiaDePac1).razon())
                .contains("relación");
    }

    @Test
    void elEspecialistaNecesitaRelacionYSuEspecialidadCuandoElRecursoLaExige() {
        assertThat(politica.evaluar(especialista("Cardiología", Set.of("pac-1")), cardiologiaDePac1).permitido()).isTrue();
        assertThat(politica.evaluar(especialista("Neumología", Set.of("pac-1")), cardiologiaDePac1).razon())
                .contains("otra especialidad");
        assertThat(politica.evaluar(especialista("Neumología", Set.of("pac-1")), historiaDePac1).permitido()).isTrue();
        assertThat(politica.evaluar(especialista("Cardiología", Set.of()), cardiologiaDePac1).permitido()).isFalse();
    }

    @Test
    void cadaRolExigeSusAtributos() {
        assertThatThrownBy(() -> medicoRural(null, Set.of())).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> especialista(" ", Set.of())).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void unaDenegacionSeConvierteEnAccesoDenegado() {
        Usuario usuario = medicoRural("centro-b", Set.of("pac-1"));
        DecisionAcceso decision = politica.evaluar(usuario, historiaDePac1);
        Instant ahora = Instant.parse("2026-11-20T10:00:00Z");

        AccesoDenegado evento = AccesoDenegado.de(usuario, historiaDePac1, decision, ahora);

        assertThat(evento.recurso()).isEqualTo("episodio/1");
        assertThat(evento.razon()).isEqualTo(decision.razon());
        assertThatThrownBy(() -> AccesoDenegado.de(usuario, historiaDePac1, DecisionAcceso.permitir("ok"), ahora))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
