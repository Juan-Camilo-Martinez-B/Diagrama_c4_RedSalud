package com.redsaludandina.identidad.dominio.servicio;

import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.identidad.dominio.modelo.AtributosAcceso;
import com.redsaludandina.identidad.dominio.modelo.DecisionAcceso;
import com.redsaludandina.identidad.dominio.modelo.RecursoClinico;
import com.redsaludandina.identidad.dominio.modelo.Usuario;

/**
 * Autorización por rol Y atributo (centro, especialidad, relación con el paciente), evaluada en el
 * punto de acceso al dato y no solo en la interfaz (Caso §5).
 *
 * <ul>
 *   <li>Paciente: solo su propia historia.</li>
 *   <li>Médico rural: pacientes de su centro con los que tiene relación de atención.</li>
 *   <li>Especialista: pacientes con los que tiene relación de atención y, si el recurso es de una
 *       especialidad, solo la suya.</li>
 * </ul>
 */
public final class PoliticaAcceso {

    public DecisionAcceso evaluar(Usuario usuario, RecursoClinico recurso) {
        Requisitos.noNulo(usuario, "usuario");
        Requisitos.noNulo(recurso, "recurso");
        AtributosAcceso atributos = usuario.atributos();
        switch (usuario.rol()) {
            case PACIENTE:
                return usuario.id().equals(recurso.pacienteId())
                        ? DecisionAcceso.permitir("Es su propia historia clínica")
                        : DecisionAcceso.denegar("Un paciente solo accede a su propia historia clínica");
            case MEDICO_RURAL:
                if (!atributos.centro().map(recurso.centroId()::equals).orElse(false)) {
                    return DecisionAcceso.denegar("El recurso es de otro centro");
                }
                return exigirRelacion(atributos, recurso);
            case ESPECIALISTA:
                boolean especialidadValida = recurso.especialidadRequerida()
                        .map(requerida -> atributos.especialidadDeclarada().map(requerida::equals).orElse(false))
                        .orElse(true);
                if (!especialidadValida) {
                    return DecisionAcceso.denegar("El recurso es de otra especialidad");
                }
                return exigirRelacion(atributos, recurso);
            default:
                return DecisionAcceso.denegar("Rol sin política de acceso");
        }
    }

    private static DecisionAcceso exigirRelacion(AtributosAcceso atributos, RecursoClinico recurso) {
        return atributos.tieneRelacionCon(recurso.pacienteId())
                ? DecisionAcceso.permitir("Tiene relación de atención con el paciente")
                : DecisionAcceso.denegar("No tiene relación de atención con el paciente");
    }
}
