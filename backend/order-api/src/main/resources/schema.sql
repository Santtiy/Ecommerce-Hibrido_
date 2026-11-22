-- Esquema de order-api. Se ejecuta en cada arranque; IF NOT EXISTS lo hace seguro de repetir.
-- Las reglas de negocio se validan en el dominio y, además, aquí con CHECK.

CREATE TABLE IF NOT EXISTS ordenes (
    id           uuid          PRIMARY KEY,
    cliente      varchar(120)  NOT NULL,
    estado       varchar(20)   NOT NULL CHECK (estado IN ('CREADA', 'PAGADA', 'CANCELADA')),
    total_monto  numeric(19,2) NOT NULL CHECK (total_monto >= 0),
    total_moneda varchar(3)    NOT NULL,
    creada_en    timestamptz   NOT NULL
);

CREATE TABLE IF NOT EXISTS items_orden (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    orden_id      uuid          NOT NULL REFERENCES ordenes (id) ON DELETE CASCADE,
    sku           varchar(60)   NOT NULL,
    nombre        varchar(120)  NOT NULL,
    cantidad      integer       NOT NULL CHECK (cantidad > 0),
    precio_monto  numeric(19,2) NOT NULL CHECK (precio_monto > 0),
    precio_moneda varchar(3)    NOT NULL
);

-- Bandeja de salida (patrón outbox): eventos guardados junto con la orden y aún no publicados.
CREATE TABLE IF NOT EXISTS bandeja_salida (
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topico    varchar(60) NOT NULL,
    clave     varchar(60) NOT NULL,
    contenido text        NOT NULL,
    creado_en timestamptz NOT NULL
);

-- Auditoría: un resultado de cada worker por orden. UNIQUE la hace idempotente.
CREATE TABLE IF NOT EXISTS auditoria_ordenes (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    orden_id      uuid         NOT NULL,
    origen        varchar(30)  NOT NULL,
    status        varchar(30)  NOT NULL,
    detalle       varchar(255),
    ocurrido_en   timestamptz  NOT NULL,
    registrado_en timestamptz  NOT NULL,
    UNIQUE (orden_id, origen, status)
);
