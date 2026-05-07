package com.irrigacao.application.service;

import com.irrigacao.domain.enums.TipoSensor;
import com.irrigacao.domain.model.LeituraSensor;
import com.irrigacao.domain.model.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GerenciadorSensores {
    private static final Logger logger = LoggerFactory.getLogger(GerenciadorSensores.class);

    private final Map<String, Sensor> sensores = new HashMap<>();
    private final Map<String, LeituraSensor> ultimasLeituras = new HashMap<>();

    public void registrarSensor(Sensor sensor) {
        sensores.put(sensor.getId(), sensor);
        logger.info("Sensor registrado: {}", sensor);
    }

    public void registrarLeitura(LeituraSensor leitura) {
        String sensorId = leitura.getSensor().getId();

        if (!leitura.isValida()) {
            logger.warn("Leitura invalida descartada do sensor {}: {}", sensorId, leitura.getValor());
            return;
        }

        ultimasLeituras.put(sensorId, leitura);
        leitura.getSensor().setUltimaLeitura(leitura.getTimestamp());
    }

    public Optional<LeituraSensor> getUltimaLeitura(String sensorId) {
        return Optional.ofNullable(ultimasLeituras.get(sensorId));
    }

    public Optional<Double> getUltimaLeituraPorTipo(TipoSensor tipo) {
        return ultimasLeituras.values().stream()
                .filter(l -> l.getSensor().getTipo() == tipo)
                .max(Comparator.comparing(LeituraSensor::getTimestamp))
                .map(LeituraSensor::getValor);
    }

    public List<Sensor> getSensoresAtivos() {
        return sensores.values().stream()
                .filter(Sensor::isAtivo)
                .toList();
    }

    public int getTotalSensores() {
        return sensores.size();
    }
}
