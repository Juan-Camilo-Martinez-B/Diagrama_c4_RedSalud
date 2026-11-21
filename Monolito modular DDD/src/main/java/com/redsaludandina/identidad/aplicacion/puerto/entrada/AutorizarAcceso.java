package com.redsaludandina.identidad.aplicacion.puerto.entrada;

import com.redsaludandina.compartido.aplicacion.AccesoNoAutorizadoException;

/**
 * Servicio de Host Abierto de autorización: los demás contextos preguntan aquí, en el punto de
 * acceso al dato, si un usuario puede ver un recurso clínico (Caso §5).
 */
public interface AutorizarAcceso {

    ResultadoAutorizacion autorizar(SolicitudAcceso solicitud);

    /** Autoriza o lanza {@link AccesoNoAutorizadoException} con la razón de la política. */
    default void exigir(SolicitudAcceso solicitud) {
        ResultadoAutorizacion resultado = autorizar(solicitud);
        if (!resultado.permitido()) {
            throw new AccesoNoAutorizadoException(resultado.razon());
        }
    }

    /** {@code especialidad} es nula si el recurso no pertenece a una especialidad. */
    record SolicitudAcceso(String usuarioId, String recurso, String pacienteId, String centroId,
                           String especialidad) {
    }

    record ResultadoAutorizacion(boolean permitido, String razon) {
    }
}
