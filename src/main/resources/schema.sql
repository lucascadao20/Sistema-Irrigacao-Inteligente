CREATE TABLE IF NOT EXISTS leitura_sensor (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id   VARCHAR(20)  NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    valor       DOUBLE       NOT NULL,
    valida      BOOLEAN      NOT NULL,
    recebido_em TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS irrigacao (
    id              VARCHAR(8)  PRIMARY KEY,
    cultura_nome    VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    volume_agua     DOUBLE      NOT NULL,
    motivo          VARCHAR(500) NOT NULL,
    estrategia_nome VARCHAR(50),
    umidade_solo    DOUBLE      NOT NULL,
    decidido_em     TIMESTAMP   NOT NULL
);
