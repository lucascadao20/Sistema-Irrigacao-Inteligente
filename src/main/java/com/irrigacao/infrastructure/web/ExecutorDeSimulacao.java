package com.irrigacao.infrastructure.web;

import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.application.servico.ServicoDeCicloIrrigacao;
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
    private final ServicoDeClima servicoDeClima;
    private final EstadoDoDashboard state;
    private final String cidade;
    private final String pais;
    private final ScheduledExecutorService executor;

    private double ultimoVolume = 0;
    private DadosClimaticos ultimoClima;

    public ExecutorDeSimulacao(ServicoDeCicloIrrigacao cicloService, SimuladorSensores simulador,
                            ServicoDeClima servicoDeClima, EstadoDoDashboard state,
                            String cidade, String pais) {
        this.cicloService = cicloService;
        this.simulador = simulador;
        this.servicoDeClima = servicoDeClima;
        this.state = state;
        this.cidade = cidade;
        this.pais = pais;
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

    private void executarCiclo() {
        try {
            double umidade = simulador.simularLeituraProgressiva(ultimoClima, ultimoVolume);
            String cultura = state.getCulturaAtiva();
            Irrigacao resultado = cicloService.executarCiclo(cultura, umidade);
            DadosClimaticos clima = servicoDeClima.obterDados(cidade, pais);

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
