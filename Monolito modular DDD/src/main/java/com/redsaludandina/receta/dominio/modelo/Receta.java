package com.redsaludandina.receta.dominio.modelo;

import com.redsaludandina.compartido.dominio.AgregadoRaiz;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.dominio.ReglaDeNegocioException;
import com.redsaludandina.compartido.dominio.Requisitos;
import com.redsaludandina.compartido.evento.RecetaFirmada;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Raíz del agregado Receta (contexto Receta). Referencia su episodio solo por identificador.
 * Firmada, es inmutable: un cambio sobre una receta firmada no se resuelve en el borde, se
 * rechaza (Caso §4). El envío a la farmacia es eventual y solo existe para recetas firmadas.
 */
public final class Receta extends AgregadoRaiz<IdClinico> {

    public static final String RECETA_FIRMADA_INMUTABLE = "RECETA_FIRMADA_INMUTABLE";
    public static final String RECETA_SIN_MEDICAMENTOS = "RECETA_SIN_MEDICAMENTOS";
    public static final String FIRMANTE_NO_ES_PRESCRIPTOR = "FIRMANTE_NO_ES_PRESCRIPTOR";
    public static final String RECETA_NO_FIRMADA = "RECETA_NO_FIRMADA";

    private final IdClinico id;
    private final IdClinico episodioId;
    private final String prescriptorId;
    private final List<Medicamento> medicamentos;
    private FirmaDigital firma;
    private Instant enviadaAFarmaciaEn;

    private Receta(IdClinico id, IdClinico episodioId, String prescriptorId, Collection<Medicamento> medicamentos,
                   FirmaDigital firma, Instant enviadaAFarmaciaEn) {
        this.id = Requisitos.noNulo(id, "recetaId");
        this.episodioId = Requisitos.noNulo(episodioId, "episodioId");
        this.prescriptorId = Requisitos.texto(prescriptorId, "prescriptorId");
        this.medicamentos = new ArrayList<>(Requisitos.noNulo(medicamentos, "medicamentos"));
        this.firma = firma;
        this.enviadaAFarmaciaEn = enviadaAFarmaciaEn;
    }

    /** Abre una receta en borrador dentro de un episodio. */
    public static Receta redactar(IdClinico id, IdClinico episodioId, String prescriptorId) {
        return new Receta(id, episodioId, prescriptorId, List.of(), null, null);
    }

    /** Reconstruye una receta ya guardada; no emite eventos. */
    public static Receta reconstituir(IdClinico id, IdClinico episodioId, String prescriptorId,
                                      Collection<Medicamento> medicamentos, FirmaDigital firma,
                                      Instant enviadaAFarmaciaEn) {
        return new Receta(id, episodioId, prescriptorId, medicamentos, firma, enviadaAFarmaciaEn);
    }

    public void agregarMedicamento(Medicamento medicamento) {
        Requisitos.noNulo(medicamento, "medicamento");
        exigirBorrador();
        medicamentos.add(medicamento);
    }

    /**
     * Firma la receta y publica RecetaFirmada. Solo firma quien la prescribió y debe tener al menos
     * un medicamento. Reenviar la misma firma no tiene efecto.
     */
    public void firmar(FirmaDigital nuevaFirma) {
        Requisitos.noNulo(nuevaFirma, "firma");
        if (firma != null) {
            if (firma.equals(nuevaFirma)) {
                return;
            }
            throw new ReglaDeNegocioException(RECETA_FIRMADA_INMUTABLE,
                    "La receta " + id + " ya está firmada y no admite otra firma");
        }
        if (medicamentos.isEmpty()) {
            throw new ReglaDeNegocioException(RECETA_SIN_MEDICAMENTOS,
                    "La receta " + id + " no tiene medicamentos y no se puede firmar");
        }
        if (!nuevaFirma.firmanteId().equals(prescriptorId)) {
            throw new ReglaDeNegocioException(FIRMANTE_NO_ES_PRESCRIPTOR,
                    "Solo el prescriptor puede firmar la receta " + id);
        }
        firma = nuevaFirma;
        registrar(new RecetaFirmada(id, episodioId, nuevaFirma.firmanteId(), nuevaFirma.firmadaEn()));
    }

    /** Marca la entrega a la farmacia del centro. Idempotente: repetirla no cambia la fecha. */
    public void marcarEnviadaAFarmacia(Instant ahora) {
        Requisitos.noNulo(ahora, "ahora");
        if (firma == null) {
            throw new ReglaDeNegocioException(RECETA_NO_FIRMADA,
                    "La receta " + id + " no se puede enviar a la farmacia sin firma");
        }
        if (enviadaAFarmaciaEn == null) {
            enviadaAFarmaciaEn = ahora;
        }
    }

    private void exigirBorrador() {
        if (firma != null) {
            throw new ReglaDeNegocioException(RECETA_FIRMADA_INMUTABLE,
                    "La receta " + id + " ya está firmada y no se puede modificar");
        }
    }

    public EstadoReceta estado() {
        return firma == null ? EstadoReceta.BORRADOR : EstadoReceta.FIRMADA;
    }

    @Override
    public IdClinico id() {
        return id;
    }

    public IdClinico episodioId() {
        return episodioId;
    }

    public String prescriptorId() {
        return prescriptorId;
    }

    public List<Medicamento> medicamentos() {
        return List.copyOf(medicamentos);
    }

    public Optional<FirmaDigital> firma() {
        return Optional.ofNullable(firma);
    }

    public Optional<Instant> enviadaAFarmaciaEn() {
        return Optional.ofNullable(enviadaAFarmaciaEn);
    }
}
