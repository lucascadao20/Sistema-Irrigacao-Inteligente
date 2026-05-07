package com.irrigacao.application.service;

import com.irrigacao.domain.enums.TipoSensor;
import com.irrigacao.domain.model.LeituraSensor;
import com.irrigacao.domain.model.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class ProcessadorDados {
    private static final Logger logger = LoggerFactory.getLogger(ProcessadorDados.class);

    private final GerenciadorSensores gerenciadorSensores;

    public ProcessadorDados(GerenciadorSensores gerenciadorSensores) {
        this.gerenciadorSensores = gerenciadorSensores;
    }

    public void processarLeitura(Sensor sensor, double valor) {
        LeituraSensor leitura = new LeituraSensor(sensor, valor, LocalDateTime.now());
        gerenciadorSensores.registrarLeitura(leitura);

        if (!leitura.isValida()) {
            logger.warn("Dado inconsistente do sensor {}: {} (ignorado, usando ultimo valido)",
                    sensor.getId(), valor);
        }
    }

    public double getUmidadeSolo() {
        return gerenciadorSensores.getUltimaLeituraPorTipo(TipoSensor.UMIDADE_SOLO)
                .orElse(50.0);
    }
}
