package com.redsaludandina.identidad.dominio.modelo;

import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.Requisitos;
import java.util.Objects;

/**
 * Raíz del agregado Usuario (contexto Identidad y acceso). Su id es el del proveedor de identidad
 * (Cognito); para un paciente coincide con su id de paciente.
 */
public final class Usuario {

    public static final String ATRIBUTOS_INCOMPLETOS = "ATRIBUTOS_INCOMPLETOS";

    private final String id;
    private final Rol rol;
    private final AtributosAcceso atributos;

    public Usuario(String id, Rol rol, AtributosAcceso atributos) {
        this.id = Requisitos.texto(id, "usuarioId");
        this.rol = Requisitos.noNulo(rol, "rol");
        this.atributos = Requisitos.noNulo(atributos, "atributos");
        exigirAtributosDelRol();
    }

    /** Un médico rural pertenece a un centro; un especialista declara su especialidad. */
    private void exigirAtributosDelRol() {
        if (rol == Rol.MEDICO_RURAL && atributos.centro().isEmpty()) {
            throw new ReglaDeNegocioException(ATRIBUTOS_INCOMPLETOS, "Un médico rural debe tener centro");
        }
        if (rol == Rol.ESPECIALISTA && atributos.especialidadDeclarada().isEmpty()) {
            throw new ReglaDeNegocioException(ATRIBUTOS_INCOMPLETOS, "Un especialista debe tener especialidad");
        }
    }

    public String id() {
        return id;
    }

    public Rol rol() {
        return rol;
    }

    public AtributosAcceso atributos() {
        return atributos;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Usuario usuario && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
