package com.irrigacao.application.servico;

import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class ProcessadorDeDados {
    private static final Logger logger = LoggerFactory.getLogger(ProcessadorDeDados.class);

    private final GerenciadorDeSensores gerenciadorDeSensores;

    public ProcessadorDeDados(GerenciadorDeSensores gerenciadorDeSensores) {
        this.gerenciadorDeSensores = gerenciadorDeSensores;
    }

    public void processarLeitura(Sensor sensor, double valor) {
        LeituraSensor leitura = new LeituraSensor(sensor, valor, LocalDateTime.now());
        gerenciadorDeSensores.registrarLeitura(leitura);

        if (!leitura.isValida()) {
            logger.warn("Dado inconsistente do sensor {}: {} (ignorado, usando ultimo valido)",
                    sensor.getId(), valor);
        }
    }

    public double getUmidadeSolo() {
        return gerenciadorDeSensores.getUltimaLeituraPorTipo(TipoSensor.UMIDADE_SOLO)
                .orElse(50.0);
    }
}
