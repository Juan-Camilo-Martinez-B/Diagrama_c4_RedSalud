-- Diagrama de secuencia de sincronización:
--  * La Idempotency-Key es del LOTE y se guarda PERSISTIDA con la respuesta R que se devolvió,
--    para responder lo mismo en cada reintento (corte de red, otra instancia, reinicio).
--  * Cada consumidor descarta eventos repetidos por su id (inbox en su propio esquema).

DROP TABLE sincronizacion.idempotencia;

CREATE TABLE sincronizacion.idempotencia (
    clave         VARCHAR(100)             PRIMARY KEY,
    huella        CHAR(64)                 NOT NULL,
    respuesta     VARCHAR(1000000)         NOT NULL,
    registrada_en TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE interconsulta.inbox (
    consumidor VARCHAR(100) NOT NULL,
    evento_id  UUID         NOT NULL,
    PRIMARY KEY (consumidor, evento_id)
);

CREATE TABLE receta.inbox (
    consumidor VARCHAR(100) NOT NULL,
    evento_id  UUID         NOT NULL,
    PRIMARY KEY (consumidor, evento_id)
);
