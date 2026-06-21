package com.irrigacao.dados.mqtt;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EstadoUltimasLeituras {
    private final ConcurrentHashMap<TipoSensor, LeituraSensor> leituras = new ConcurrentHashMap<>();

    public void registrar(Sensor sensor, double valor, LocalDateTime timestamp) {
        LeituraSensor leitura = new LeituraSensor(sensor, valor, timestamp);
        leituras.put(sensor.getTipo(), leitura);
    }

    public Optional<LeituraSensor> getUltima(TipoSensor tipo) {
        return Optional.ofNullable(leituras.get(tipo));
    }
}
