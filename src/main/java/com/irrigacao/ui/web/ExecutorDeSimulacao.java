package com.irrigacao.ui.web;

import com.irrigacao.negocio.ServicoDeCicloIrrigacao;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.dados.SimuladorSensores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorDeSimulacao {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorDeSimulacao.class);

    private final ServicoDeCicloIrrigacao cicloService;
    private final SimuladorSensores simulador;
    private final EstadoDoDashboard state;
    private final ScheduledExecutorService executor;

    private double ultimoVolume = 0;
    private DadosClimaticos ultimoClima;

    public ExecutorDeSimulacao(ServicoDeCicloIrrigacao cicloService, SimuladorSensores simulador,
                            EstadoDoDashboard state) {
        this.cicloService = cicloService;
        this.simulador = simulador;
        this.state = state;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "executor-simulacao");
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

    /** Agenda a execucao imediata de um ciclo, sem esperar o proximo intervalo. */
    public void dispararCicloImediato() {
        executor.execute(this::executarCiclo);
    }

    private void executarCiclo() {
        try {
            double umidade = simulador.simularLeituraProgressiva(ultimoClima, ultimoVolume);
            String cultura = state.getCulturaAtiva();
            Irrigacao resultado = cicloService.executarCiclo(cultura, umidade);
            DadosClimaticos clima = cicloService.getUltimoClima();

            String estrategia = cicloService.getMotorDeRegras().getEstrategiaAtual() != null
                    ? cicloService.getMotorDeRegras().getEstrategiaAtual().getNome()
                    : "Aguardando";

            state.registrarCiclo(umidade, clima, resultado, estrategia);
            ultimoVolume = resultado.getVolumeAgua();
            ultimoClima = clima;
        } catch (Exception e) {
            logger.error("Erro no ciclo de simulacao", e);
        }
    }
}
