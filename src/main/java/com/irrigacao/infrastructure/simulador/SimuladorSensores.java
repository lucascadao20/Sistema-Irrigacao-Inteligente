package com.irrigacao.infrastructure.simulador;

import com.irrigacao.application.servico.GerenciadorDeSensores;
import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class SimuladorSensores {
    private static final Logger logger = LoggerFactory.getLogger(SimuladorSensores.class);

    private final Random random = new Random();
    private final GerenciadorDeSensores gerenciador;

    private double umidadeAtual;
    private boolean inicializado = false;

    private static final double TAXA_EVAPORACAO_BASE = 0.8;
    private static final double FATOR_TEMPERATURA = 0.05;
    private static final double FATOR_VENTO = 0.15;
    private static final double ABSORCAO_CHUVA = 0.7;
    private static final double GANHO_IRRIGACAO = 0.8;
    private static final double VARIACAO_SENSOR = 0.5;

    public SimuladorSensores(GerenciadorDeSensores gerenciador) {
        this.gerenciador = gerenciador;
    }

    public void inicializarSensores() {
        Sensor sensorUmidade = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A - Norte");
        Sensor sensorTemp = new Sensor("ST-001", TipoSensor.TEMPERATURA, "Talhao A - Centro");
        Sensor sensorUmidadeAr = new Sensor("SA-001", TipoSensor.UMIDADE_AR, "Talhao A - Estacao");
        Sensor sensorPH = new Sensor("SP-001", TipoSensor.PH_SOLO, "Talhao A - Sul");

        gerenciador.registrarSensor(sensorUmidade);
        gerenciador.registrarSensor(sensorTemp);
        gerenciador.registrarSensor(sensorUmidadeAr);
        gerenciador.registrarSensor(sensorPH);

        logger.info("Sensores IoT inicializados. Total: {}", gerenciador.getTotalSensores());
    }

    public double simularLeituraProgressiva(DadosClimaticos clima, double volumeIrrigado) {
        if (!inicializado) {
            umidadeAtual = 40 + random.nextDouble() * 15;
            inicializado = true;
        }

        double perdaEvaporacao = TAXA_EVAPORACAO_BASE;

        if (clima != null) {
            if (clima.getTemperatura() > 25) {
                perdaEvaporacao += (clima.getTemperatura() - 25) * FATOR_TEMPERATURA;
            }
            perdaEvaporacao += clima.getVelocidadeVento() * FATOR_VENTO;

            if (clima.getUmidadeAr() > 70) {
                perdaEvaporacao *= 0.6;
            }

            if (clima.isPrevisaoChuva() && clima.getVolumeChuva() > 0) {
                double ganhoChuva = clima.getVolumeChuva() * ABSORCAO_CHUVA;
                umidadeAtual += ganhoChuva;
            }
        }

        if (volumeIrrigado > 0) {
            double ganhoIrrigacao = (volumeIrrigado / 10.0) * GANHO_IRRIGACAO;
            umidadeAtual += ganhoIrrigacao;
        }

        umidadeAtual -= perdaEvaporacao;

        double ruido = (random.nextDouble() - 0.5) * 2 * VARIACAO_SENSOR;
        double leitura = umidadeAtual + ruido;

        umidadeAtual = Math.max(0, Math.min(100, umidadeAtual));
        leitura = Math.max(0, Math.min(100, leitura));

        return Math.round(leitura * 10.0) / 10.0;
    }

    public double simularLeituraUmidadeSolo() {
        return 15 + random.nextDouble() * 60;
    }

    public double simularLeituraCritica() {
        return 15 + random.nextDouble() * 10;
    }

    public double simularLeituraAdequada() {
        return 50 + random.nextDouble() * 20;
    }

    public double gerarLeituraInvalida() {
        return -10 + random.nextDouble() * -50;
    }
}
