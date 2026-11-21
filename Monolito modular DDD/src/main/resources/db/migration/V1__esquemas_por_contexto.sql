-- Un esquema por contexto delimitado: cada contexto es dueño de su esquema y nadie escribe en el
-- de otro (Caso §1 y §2). Cada contexto que publica eventos tiene su propia tabla outbox.
-- SQL compatible con PostgreSQL (Aurora) y con H2 en modo PostgreSQL (perfil local y pruebas).

CREATE SCHEMA IF NOT EXISTS agenda;
CREATE SCHEMA IF NOT EXISTS episodio;
CREATE SCHEMA IF NOT EXISTS receta;
CREATE SCHEMA IF NOT EXISTS interconsulta;
CREATE SCHEMA IF NOT EXISTS identidad;
CREATE SCHEMA IF NOT EXISTS legado;
CREATE SCHEMA IF NOT EXISTS auditoria;
CREATE SCHEMA IF NOT EXISTS analitica;
CREATE SCHEMA IF NOT EXISTS sincronizacion;

-- ---------------------------------------------------------------- Agenda
CREATE TABLE agenda.cupo (
    id              VARCHAR(26)              PRIMARY KEY,
    especialista_id VARCHAR(100)             NOT NULL,
    inicio          TIMESTAMP WITH TIME ZONE NOT NULL,
    fin             TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Como máximo una reserva por cupo: la base también garantiza «sin doble reserva».
CREATE TABLE agenda.reserva (
    cupo_id      VARCHAR(26)              PRIMARY KEY REFERENCES agenda.cupo (id),
    reserva_id   VARCHAR(26)              NOT NULL UNIQUE,
    paciente_id  VARCHAR(100)             NOT NULL,
    centro_id    VARCHAR(100)             NOT NULL,
    reservada_en TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------- Episodio clínico
CREATE TABLE episodio.episodio (
    id          VARCHAR(26)              PRIMARY KEY,
    paciente_id VARCHAR(100)             NOT NULL,
    cupo_id     VARCHAR(26),
    centro_id   VARCHAR(100)             NOT NULL,
    estado      VARCHAR(20)              NOT NULL,
    atendido_en TIMESTAMP WITH TIME ZONE
);

CREATE TABLE episodio.asistencia (
    episodio_id    VARCHAR(26)              NOT NULL REFERENCES episodio.episodio (id),
    campo          VARCHAR(20)              NOT NULL,
    texto          VARCHAR(4000)            NOT NULL,
    sello_centro   VARCHAR(100)             NOT NULL,
    sello_instante TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (episodio_id, campo)
);

CREATE TABLE episodio.adjunto (
    id                   VARCHAR(26)  PRIMARY KEY,
    episodio_id          VARCHAR(26)  NOT NULL REFERENCES episodio.episodio (id),
    tipo                 VARCHAR(20)  NOT NULL,
    clave_almacenamiento VARCHAR(500) NOT NULL,
    sha256               VARCHAR(64)  NOT NULL
);

-- ---------------------------------------------------------------- Receta
CREATE TABLE receta.receta (
    id                    VARCHAR(26)              PRIMARY KEY,
    episodio_id           VARCHAR(26)              NOT NULL,
    prescriptor_id        VARCHAR(100)             NOT NULL,
    firmante_id           VARCHAR(100),
    firmada_en            TIMESTAMP WITH TIME ZONE,
    firma_valor           VARCHAR(4000),
    enviada_a_farmacia_en TIMESTAMP WITH TIME ZONE
);

CREATE TABLE receta.medicamento (
    receta_id  VARCHAR(26)  NOT NULL REFERENCES receta.receta (id),
    posicion   INTEGER      NOT NULL,
    nombre     VARCHAR(200) NOT NULL,
    dosis      VARCHAR(200) NOT NULL,
    indicacion VARCHAR(500) NOT NULL,
    PRIMARY KEY (receta_id, posicion)
);

-- Registro local de envíos a farmacia (perfil sin AWS; en AWS es una cola SQS).
CREATE TABLE receta.envio_farmacia (
    receta_id  VARCHAR(26)              PRIMARY KEY,
    enviado_en TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------- Interconsulta
CREATE TABLE interconsulta.interconsulta (
    id                   VARCHAR(26)  PRIMARY KEY,
    episodio_origen_id   VARCHAR(26)  NOT NULL,
    paciente_id          VARCHAR(100) NOT NULL,
    centro_id            VARCHAR(100) NOT NULL,
    especialidad         VARCHAR(100) NOT NULL,
    motivo               VARCHAR(1000) NOT NULL,
    estado               VARCHAR(20)  NOT NULL,
    cupo_id              VARCHAR(26),
    episodio_atencion_id VARCHAR(26)
);

CREATE TABLE interconsulta.alerta (
    interconsulta_id VARCHAR(26)              NOT NULL REFERENCES interconsulta.interconsulta (id),
    posicion         INTEGER                  NOT NULL,
    tipo             VARCHAR(30)              NOT NULL,
    creada_en        TIMESTAMP WITH TIME ZONE NOT NULL,
    resuelta_en      TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (interconsulta_id, posicion)
);

-- ---------------------------------------------------------------- Identidad y acceso
-- Perfiles y atributos (en AWS: tabla de perfiles en DynamoDB; la identidad la valida Cognito).
CREATE TABLE identidad.usuario (
    id           VARCHAR(100) PRIMARY KEY,
    rol          VARCHAR(20)  NOT NULL,
    centro_id    VARCHAR(100),
    especialidad VARCHAR(100)
);

CREATE TABLE identidad.paciente_relacionado (
    usuario_id  VARCHAR(100) NOT NULL REFERENCES identidad.usuario (id),
    paciente_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (usuario_id, paciente_id)
);

-- ---------------------------------------------------------------- Integración legado
-- Read model canónico del historial con TTL explícito (en AWS: ElastiCache/DynamoDB).
CREATE TABLE legado.historial_read_model (
    paciente_id VARCHAR(100)             PRIMARY KEY,
    contenido   VARCHAR(1000000)         NOT NULL,
    obtenido_en TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_en   TIMESTAMP WITH TIME ZONE NOT NULL,
    origen      VARCHAR(20)              NOT NULL
);

CREATE TABLE legado.estado_job (
    nombre         VARCHAR(100)             PRIMARY KEY,
    ultima_corrida TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------- Auditoría
-- Append-only: la aplicación solo inserta (en AWS: S3 Object Lock en modo compliance, ADR-04).
CREATE TABLE auditoria.evento_acceso (
    id          UUID                     PRIMARY KEY,
    tipo        VARCHAR(30)              NOT NULL,
    usuario_id  VARCHAR(100)             NOT NULL,
    recurso     VARCHAR(200)             NOT NULL,
    motivo      VARCHAR(1000)            NOT NULL,
    ocurrido_en TIMESTAMP WITH TIME ZONE NOT NULL,
    detalle     VARCHAR(8000)            NOT NULL
);

CREATE INDEX evento_acceso_recurso ON auditoria.evento_acceso (recurso);

-- ---------------------------------------------------------------- Analítica de uso
-- Solo conteos por centro y mes: ningún identificador de paciente (seudonimizado por diseño).
CREATE TABLE analitica.indicador (
    centro_id                  VARCHAR(100) NOT NULL,
    periodo                    VARCHAR(7)   NOT NULL,
    teleconsultas              BIGINT       NOT NULL,
    reservas                   BIGINT       NOT NULL,
    espera_acumulada_segundos  BIGINT       NOT NULL,
    interconsultas_solicitadas BIGINT       NOT NULL,
    interconsultas_cerradas    BIGINT       NOT NULL,
    PRIMARY KEY (centro_id, periodo)
);

CREATE TABLE analitica.inbox (
    consumidor VARCHAR(100) NOT NULL,
    evento_id  UUID         NOT NULL,
    PRIMARY KEY (consumidor, evento_id)
);

-- ---------------------------------------------------------------- Sincronización offline
CREATE TABLE sincronizacion.idempotencia (
    clave   VARCHAR(26)   PRIMARY KEY,
    estado  VARCHAR(20)   NOT NULL,
    codigo  VARCHAR(100),
    mensaje VARCHAR(1000)
);
