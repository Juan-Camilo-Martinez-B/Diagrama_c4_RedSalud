package com.redsaludandina.episodio.aplicacion;

import com.redsaludandina.compartido.aplicacion.Fijos;
import com.redsaludandina.compartido.dominio.IdClinico;
import com.redsaludandina.episodio.aplicacion.puerto.salida.AlmacenAdjuntosPort;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Almacén de adjuntos de prueba: claves deterministas y URLs ficticias. */
public final class AlmacenAdjuntosDePrueba implements AlmacenAdjuntosPort {

    @Override
    public String claveDe(IdClinico episodioId, IdClinico adjuntoId) {
        return "episodios/" + episodioId + "/adjuntos/" + adjuntoId;
    }

    @Override
    public SubidaPrefirmada prepararSubida(IdClinico episodioId, IdClinico adjuntoId, String sha256) {
        return new SubidaPrefirmada(URI.create("https://almacen.prueba/subida/" + adjuntoId),
                Map.of("x-amz-checksum-sha256", List.of(sha256)), claveDe(episodioId, adjuntoId), Fijos.AHORA);
    }

    @Override
    public DescargaPrefirmada prepararDescarga(String claveAlmacenamiento) {
        return new DescargaPrefirmada(URI.create("https://almacen.prueba/descarga/" + claveAlmacenamiento), Fijos.AHORA);
    }
}
