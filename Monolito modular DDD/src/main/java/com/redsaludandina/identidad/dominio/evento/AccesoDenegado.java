package com.redsaludandina.identidad.dominio.evento;

import com.redsaludandina.compartido.dominio.EventoDominio;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.identidad.dominio.modelo.DecisionAcceso;
import com.redsaludandina.identidad.dominio.modelo.RecursoClinico;
import com.redsaludandina.identidad.dominio.modelo.Usuario;
import java.time.Instant;

/** Se negó el acceso de un usuario a un recurso clínico, con la razón de la política. */
public record AccesoDenegado(String usuarioId, String recurso, String razon, Instant ocurridoEn)
        implements EventoDominio {

    public AccesoDenegado {
        usuarioId = Requisitos.texto(usuarioId, "usuarioId");
        recurso = Requisitos.texto(recurso, "recurso");
        razon = Requisitos.texto(razon, "razon");
        Requisitos.noNulo(ocurridoEn, "ocurridoEn");
    }

    public static AccesoDenegado de(Usuario usuario, RecursoClinico recurso, DecisionAcceso decision, Instant ahora) {
        if (decision.permitido()) {
            throw new IllegalArgumentException("Solo una decisión denegada produce AccesoDenegado");
        }
        return new AccesoDenegado(usuario.id(), recurso.referencia(), decision.razon(), ahora);
    }
}
