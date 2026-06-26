package com.irrigacao.dados.mqtt;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EstadoUltimasLeituras {

    public static final String GLOBAL = "";

    private record Chave(TipoSensor tipo, String cultura) {}

    private final ConcurrentHashMap<Chave, LeituraSensor> leituras = new ConcurrentHashMap<>();

    public void registrar(Sensor sensor, double valor, LocalDateTime timestamp) {
        registrar(sensor, valor, timestamp, GLOBAL);
    }

    public void registrar(Sensor sensor, double valor, LocalDateTime timestamp, String cultura) {
        leituras.put(new Chave(sensor.getTipo(), cultura),
                new LeituraSensor(sensor, valor, timestamp));
    }

    public Optional<LeituraSensor> getUltima(TipoSensor tipo) {
        return getUltima(tipo, GLOBAL);
    }

    public Optional<LeituraSensor> getUltima(TipoSensor tipo, String cultura) {
        return Optional.ofNullable(leituras.get(new Chave(tipo, cultura)));
    }
}
