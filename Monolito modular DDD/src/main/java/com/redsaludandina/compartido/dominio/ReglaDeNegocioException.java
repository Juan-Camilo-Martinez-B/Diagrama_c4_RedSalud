package com.redsaludandina.compartido.dominio;

/**
 * Una regla de negocio impidió la operación. El {@link #codigo()} es estable para que los
 * adaptadores lo traduzcan (por ejemplo, la sincronización offline lo devuelve como
 * «rechazado, reintentar contra el servidor»).
 */
public class ReglaDeNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaDeNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = Requisitos.texto(codigo, "codigo");
    }

    public String codigo() {
        return codigo;
    }
}
