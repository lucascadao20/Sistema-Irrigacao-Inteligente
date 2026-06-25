package com.irrigacao.dados.mqtt;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache thread-safe das ultimas leituras recebidas via MQTT.
 *
 * <p>Algumas leituras (umidade do solo) sao mantidas <strong>por cultura</strong> —
 * cada cultura tem seu proprio "talhao" simulado. Os demais sensores
 * (temperatura, umidade do ar, pH) sao globais — uma unica leitura
 * compartilhada para o sistema todo.
 */
public final class EstadoUltimasLeituras {

    private record Chave(TipoSensor tipo, String cultura) {}

    /** Marcador para sensores globais (sem associacao a cultura). */
    public static final String GLOBAL = "";

    private final ConcurrentHashMap<Chave, LeituraSensor> leituras = new ConcurrentHashMap<>();

    /** Registra leitura global (sensores nao associados a uma cultura). */
    public void registrar(Sensor sensor, double valor, LocalDateTime timestamp) {
        registrar(sensor, valor, timestamp, GLOBAL);
    }

    /** Registra leitura associada a uma cultura especifica. */
    public void registrar(Sensor sensor, double valor, LocalDateTime timestamp, String cultura) {
        LeituraSensor leitura = new LeituraSensor(sensor, valor, timestamp);
        leituras.put(new Chave(sensor.getTipo(), cultura), leitura);
    }

    /** Retorna a ultima leitura global do tipo. */
    public Optional<LeituraSensor> getUltima(TipoSensor tipo) {
        return Optional.ofNullable(leituras.get(new Chave(tipo, GLOBAL)));
    }

    /** Retorna a ultima leitura do tipo para a cultura informada. */
    public Optional<LeituraSensor> getUltima(TipoSensor tipo, String cultura) {
        return Optional.ofNullable(leituras.get(new Chave(tipo, cultura)));
    }
}
