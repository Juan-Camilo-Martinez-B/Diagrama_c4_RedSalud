package com.redsaludandina.identidad.aplicacion.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.redsaludandina.compartido.aplicacion.AccesoNoAutorizadoException;
import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.aplicacion.UnidadDeTrabajoDirecta;
import com.redsaludandina.compartido.aplicacion.mensajeria.OutboxEnMemoria;
import com.redsaludandina.identidad.aplicacion.puerto.entrada.AutorizarAcceso.SolicitudAcceso;
import com.redsaludandina.identidad.dominio.evento.AccesoDenegado;
import com.redsaludandina.identidad.dominio.modelo.AtributosAcceso;
import com.redsaludandina.identidad.dominio.modelo.Rol;
import com.redsaludandina.identidad.dominio.modelo.Usuario;
import com.redsaludandina.identidad.dominio.servicio.PoliticaAcceso;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServicioAutorizacionTest {

    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final Map<String, Usuario> usuarios = Map.of(
            "med-1", new Usuario("med-1", Rol.MEDICO_RURAL, new AtributosAcceso("centro-a", null, Set.of("pac-1"))));
    private final ServicioAutorizacion servicio = new ServicioAutorizacion(
            id -> Optional.ofNullable(usuarios.get(id)), new PoliticaAcceso(), new UnidadDeTrabajoDirecta(),
            outbox.registrador("identidad"), Fijos.RELOJ);

    @Test
    void permiteSinDejarRastroDeDenegacion() {
        var resultado = servicio.autorizar(new SolicitudAcceso("med-1", "episodio/1", "pac-1", "centro-a", null));

        assertThat(resultado.permitido()).isTrue();
        assertThat(outbox.mensajes()).isEmpty();
    }

    @Test
    void cadaDenegacionQuedaEnElOutboxComoAccesoDenegado() {
        SolicitudAcceso otroCentro = new SolicitudAcceso("med-1", "episodio/1", "pac-1", "centro-b", null);

        assertThatThrownBy(() -> servicio.exigir(otroCentro))
                .isInstanceOf(AccesoNoAutorizadoException.class)
                .hasMessageContaining("otro centro");
        assertThat(outbox.eventosDe(AccesoDenegado.class)).singleElement()
                .satisfies(evento -> assertThat(evento.recurso()).isEqualTo("episodio/1"));
    }

    @Test
    void unUsuarioSinPerfilSeDeniega() {
        var resultado = servicio.autorizar(new SolicitudAcceso("desconocido", "episodio/1", "pac-1", "centro-a", null));

        assertThat(resultado.permitido()).isFalse();
        assertThat(outbox.eventosDe(AccesoDenegado.class)).hasSize(1);
    }
}
