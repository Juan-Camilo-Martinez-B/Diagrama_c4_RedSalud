package com.redsaludandina.receta.infraestructura.adaptador.salida.persistencia;

import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.compartido.infraestructura.persistencia.Jdbc;
import com.redsaludandina.receta.aplicacion.puerto.salida.RecetaRepositorio;
import com.redsaludandina.receta.dominio.modelo.FirmaDigital;
import com.redsaludandina.receta.dominio.modelo.Medicamento;
import com.redsaludandina.receta.dominio.modelo.Receta;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Recetas en el esquema receta. Bloquea la fila raíz al leer, como el resto de agregados. */
public final class RecetaRepositorioJdbc implements RecetaRepositorio {

    private final JdbcClient jdbc;

    public RecetaRepositorioJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Receta> buscar(IdClinico recetaId) {
        List<Medicamento> medicamentos = jdbc.sql("SELECT nombre, dosis, indicacion FROM receta.medicamento"
                        + " WHERE receta_id = :id ORDER BY posicion")
                .param("id", recetaId.valor())
                .query((fila, n) -> new Medicamento(fila.getString("nombre"), fila.getString("dosis"),
                        fila.getString("indicacion")))
                .list();
        return jdbc.sql("SELECT episodio_id, prescriptor_id, firmante_id, firmada_en, firma_valor, enviada_a_farmacia_en"
                        + " FROM receta.receta WHERE id = :id FOR UPDATE")
                .param("id", recetaId.valor())
                .query((fila, n) -> {
                    String firmante = fila.getString("firmante_id");
                    FirmaDigital firma = firmante == null ? null
                            : new FirmaDigital(firmante, Jdbc.instante(fila, "firmada_en"), fila.getString("firma_valor"));
                    return Receta.reconstituir(recetaId, IdClinico.de(fila.getString("episodio_id")),
                            fila.getString("prescriptor_id"), medicamentos, firma,
                            Jdbc.instante(fila, "enviada_a_farmacia_en"));
                })
                .optional();
    }

    @Override
    public void guardar(Receta receta) {
        String id = receta.id().valor();
        String firmante = receta.firma().map(FirmaDigital::firmanteId).orElse(null);
        Instant firmadaEn = receta.firma().map(FirmaDigital::firmadaEn).orElse(null);
        String valor = receta.firma().map(FirmaDigital::valor).orElse(null);
        int actualizados = jdbc.sql("UPDATE receta.receta SET firmante_id = :firmante, firmada_en = :firmada,"
                        + " firma_valor = :valor, enviada_a_farmacia_en = :enviada WHERE id = :id")
                .param("firmante", firmante)
                .param("firmada", Jdbc.aFecha(firmadaEn))
                .param("valor", valor)
                .param("enviada", Jdbc.aFecha(receta.enviadaAFarmaciaEn().orElse(null)))
                .param("id", id)
                .update();
        if (actualizados == 0) {
            jdbc.sql("INSERT INTO receta.receta (id, episodio_id, prescriptor_id, firmante_id, firmada_en, firma_valor,"
                            + " enviada_a_farmacia_en) VALUES (:id, :episodio, :prescriptor, :firmante, :firmada, :valor, :enviada)")
                    .param("id", id)
                    .param("episodio", receta.episodioId().valor())
                    .param("prescriptor", receta.prescriptorId())
                    .param("firmante", firmante)
                    .param("firmada", Jdbc.aFecha(firmadaEn))
                    .param("valor", valor)
                    .param("enviada", Jdbc.aFecha(receta.enviadaAFarmaciaEn().orElse(null)))
                    .update();
        }
        jdbc.sql("DELETE FROM receta.medicamento WHERE receta_id = :id").param("id", id).update();
        List<Medicamento> medicamentos = receta.medicamentos();
        for (int posicion = 0; posicion < medicamentos.size(); posicion++) {
            Medicamento medicamento = medicamentos.get(posicion);
            jdbc.sql("INSERT INTO receta.medicamento (receta_id, posicion, nombre, dosis, indicacion)"
                            + " VALUES (:id, :posicion, :nombre, :dosis, :indicacion)")
                    .param("id", id)
                    .param("posicion", posicion)
                    .param("nombre", medicamento.nombre())
                    .param("dosis", medicamento.dosis())
                    .param("indicacion", medicamento.indicacion())
                    .update();
        }
    }
}
