-- Un esquema por contexto delimitado (Caso §1 y §2): cada contexto es dueño de sus datos y nadie
-- escribe en el esquema de otro. SQL compatible con PostgreSQL (Aurora) y con H2 en modo PostgreSQL.

CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS scheduling;
CREATE SCHEMA IF NOT EXISTS episode;
CREATE SCHEMA IF NOT EXISTS prescription;
CREATE SCHEMA IF NOT EXISTS referral;
CREATE SCHEMA IF NOT EXISTS legacy;
CREATE SCHEMA IF NOT EXISTS insights;

-- ---------------------------------------------------------------- Identidad y acceso
CREATE TABLE identity.user_account (
    id        VARCHAR(100) PRIMARY KEY,
    role      VARCHAR(20)  NOT NULL,
    center_id VARCHAR(100),
    specialty VARCHAR(100)
);

CREATE TABLE identity.related_patient (
    user_id    VARCHAR(100) NOT NULL REFERENCES identity.user_account (id),
    patient_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, patient_id)
);

-- ---------------------------------------------------------------- Agenda
-- La reserva vive en la misma fila del cupo: a lo sumo una por cupo, sin doble reserva.
CREATE TABLE scheduling.slot (
    id            VARCHAR(26)              PRIMARY KEY,
    specialist_id VARCHAR(100)             NOT NULL,
    starts_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    booking_id    VARCHAR(26)              UNIQUE,
    patient_id    VARCHAR(100),
    center_id     VARCHAR(100),
    booked_at     TIMESTAMP WITH TIME ZONE
);

-- ---------------------------------------------------------------- Episodio clínico (+ sincronización offline)
CREATE TABLE episode.episode (
    id          VARCHAR(26)              PRIMARY KEY,
    patient_id  VARCHAR(100)             NOT NULL,
    slot_id     VARCHAR(26),
    center_id   VARCHAR(100)             NOT NULL,
    status      VARCHAR(20)              NOT NULL,
    attended_at TIMESTAMP WITH TIME ZONE,
    version     BIGINT                   NOT NULL DEFAULT 0
);

CREATE TABLE episode.care_value (
    episode_id VARCHAR(26)              NOT NULL REFERENCES episode.episode (id),
    field      VARCHAR(20)              NOT NULL,
    text       VARCHAR(4000)            NOT NULL,
    center_id  VARCHAR(100)             NOT NULL,
    stamped_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (episode_id, field)
);

CREATE TABLE episode.attachment (
    episode_id  VARCHAR(26)  NOT NULL REFERENCES episode.episode (id),
    id          VARCHAR(26)  NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    storage_key VARCHAR(300) NOT NULL,
    sha256      VARCHAR(64)  NOT NULL,
    PRIMARY KEY (episode_id, id)
);

-- Recibo de idempotencia por lote: Idempotency-Key, huella SHA-256 del lote y respuesta R.
CREATE TABLE episode.sync_receipt (
    idempotency_key VARCHAR(100)             PRIMARY KEY,
    batch_hash      VARCHAR(64)              NOT NULL,
    result          TEXT                     NOT NULL,
    definitive      BOOLEAN                  NOT NULL,
    replayed        BOOLEAN                  NOT NULL,
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------- Receta
CREATE TABLE prescription.prescription (
    id                  VARCHAR(26)              PRIMARY KEY,
    episode_id          VARCHAR(26)              NOT NULL,
    prescriber_id       VARCHAR(100)             NOT NULL,
    signer_id           VARCHAR(100),
    signed_at           TIMESTAMP WITH TIME ZONE,
    signature_value     VARCHAR(4000),
    sent_to_pharmacy_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE prescription.medication (
    prescription_id VARCHAR(26)  NOT NULL REFERENCES prescription.prescription (id),
    position        INTEGER      NOT NULL,
    name            VARCHAR(200) NOT NULL,
    dose            VARCHAR(200) NOT NULL,
    instructions    VARCHAR(500) NOT NULL,
    PRIMARY KEY (prescription_id, position)
);

-- Farmacia simulada del perfil local: registro de envíos.
CREATE TABLE prescription.pharmacy_dispatch (
    prescription_id VARCHAR(26)              PRIMARY KEY,
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------- Interconsulta
CREATE TABLE referral.referral (
    id                   VARCHAR(26)              PRIMARY KEY,
    origin_episode_id    VARCHAR(26)              NOT NULL,
    patient_id           VARCHAR(100)             NOT NULL,
    center_id            VARCHAR(100)             NOT NULL,
    specialty            VARCHAR(100)             NOT NULL,
    reason               VARCHAR(1000)            NOT NULL,
    status               VARCHAR(20)              NOT NULL,
    slot_id              VARCHAR(26),
    attention_episode_id VARCHAR(26),
    requested_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE referral.alert (
    referral_id VARCHAR(26)              NOT NULL REFERENCES referral.referral (id),
    position    INTEGER                  NOT NULL,
    type        VARCHAR(30)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (referral_id, position)
);

-- ---------------------------------------------------------------- Integración con el legado
-- Modelo de lectura del historial (CQRS + ACL): copia traducida con TTL; el legado es la fuente de verdad.
CREATE TABLE legacy.history_read_model (
    patient_id  VARCHAR(100)             PRIMARY KEY,
    content     TEXT                     NOT NULL,
    obtained_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    source      VARCHAR(20)              NOT NULL
);

CREATE TABLE legacy.job_state (
    name     VARCHAR(100)             PRIMARY KEY,
    last_run TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ---------------------------------------------------------------- Analítica y evidencia (auditoría + analítica)
-- Registro de accesos append-only del perfil local (en AWS: S3 Object Lock, ADR-04).
CREATE TABLE insights.access_record (
    id          UUID                     PRIMARY KEY,
    type        VARCHAR(30)              NOT NULL,
    user_id     VARCHAR(100)             NOT NULL,
    resource    VARCHAR(300)             NOT NULL,
    reason      VARCHAR(1000)            NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail      VARCHAR(4000)            NOT NULL
);
CREATE INDEX access_record_by_resource ON insights.access_record (resource);

-- Indicadores de uso seudonimizados (por centro y mes), fuera del modelo transaccional.
CREATE TABLE insights.usage_indicator (
    id                  VARCHAR(120) PRIMARY KEY,
    center_id           VARCHAR(100) NOT NULL,
    period              VARCHAR(7)   NOT NULL,
    teleconsultations   BIGINT       NOT NULL,
    bookings            BIGINT       NOT NULL,
    total_wait_seconds  BIGINT       NOT NULL,
    referrals_requested BIGINT       NOT NULL,
    referrals_closed    BIGINT       NOT NULL
);
