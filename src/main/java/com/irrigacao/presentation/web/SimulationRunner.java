package com.irrigacao.presentation.web;

import com.irrigacao.application.port.ClimaService;
import com.irrigacao.application.service.CicloIrrigacaoService;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import com.irrigacao.infrastructure.simulator.SimuladorSensores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimulationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunner.class);

    private final CicloIrrigacaoService cicloService;
    private final SimuladorSensores simulador;
    private final ClimaService climaService;
    private final DashboardState state;
    private final String cidade;
    private final String pais;
    private final ScheduledExecutorService executor;

    private double ultimoVolume = 0;
    private DadosClimaticos ultimoClima;

    public SimulationRunner(CicloIrrigacaoService cicloService, SimuladorSensores simulador,
                            ClimaService climaService, DashboardState state,
                            String cidade, String pais) {
        this.cicloService = cicloService;
        this.simulador = simulador;
        this.climaService = climaService;
        this.state = state;
        this.cidade = cidade;
        this.pais = pais;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "simulation-runner");
            t.setDaemon(true);
            return t;
        });
    }

    public void iniciar(int intervaloSegundos) {
        executor.scheduleAtFixedRate(this::executarCiclo, 0, intervaloSegundos, TimeUnit.SECONDS);
        logger.info("Simulacao iniciada com intervalo de {}s", intervaloSegundos);
    }

    public void parar() {
        executor.shutdownNow();
    }

    private void executarCiclo() {
        try {
            double umidade = simulador.simularLeituraProgressiva(ultimoClima, ultimoVolume);
            String cultura = state.getCulturaAtiva();
            Irrigacao resultado = cicloService.executarCiclo(cultura, umidade);
            DadosClimaticos clima = climaService.obterDados(cidade, pais);

            String estrategia = cicloService.getMotorRegras().getStrategyAtual() != null
                    ? cicloService.getMotorRegras().getStrategyAtual().getNome()
                    : "Aguardando";

            state.registrarCiclo(umidade, clima, resultado, estrategia);
            ultimoVolume = resultado.getVolumeAgua();
            ultimoClima = clima;
        } catch (Exception e) {
            logger.error("Erro no ciclo de simulacao", e);
        }
    }
}
