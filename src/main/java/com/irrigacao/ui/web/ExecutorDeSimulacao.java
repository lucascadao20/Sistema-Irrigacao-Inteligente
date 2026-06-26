package com.irrigacao.ui.web;

import com.irrigacao.dados.mqtt.EstadoUltimasLeituras;
import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.NivelAlerta;
import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.negocio.ServicoDeCicloIrrigacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorDeSimulacao {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorDeSimulacao.class);

    private final ServicoDeCicloIrrigacao cicloService;
    private final EstadoUltimasLeituras leituras;
    private final EstadoDoDashboard state;
    private final ScheduledExecutorService executor;

    private boolean avisadoAusencia = false;

    public ExecutorDeSimulacao(ServicoDeCicloIrrigacao cicloService,
                                EstadoUltimasLeituras leituras,
                                EstadoDoDashboard state) {
        this.cicloService = cicloService;
        this.leituras = leituras;
        this.state = state;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "executor-simulacao");
            t.setDaemon(true);
            return t;
        });
    }

    public void iniciar(int intervaloSegundos) {
        executor.scheduleAtFixedRate(this::executarCiclo, 0, intervaloSegundos, TimeUnit.SECONDS);
        logger.info("Ciclo iniciado com intervalo de {}s (fonte: MQTT)", intervaloSegundos);
    }

    public void parar() {
        executor.shutdownNow();
    }

    public void dispararCicloImediato() {
        executor.execute(this::executarCiclo);
    }

    void executarCiclo() {
        try {
            String cultura = state.getCulturaAtiva();
            Optional<LeituraSensor> ultima = leituras.getUltima(TipoSensor.UMIDADE_SOLO, cultura);
            if (ultima.isEmpty()) {
                if (!avisadoAusencia) {
                    state.registrarAlerta(new Alerta(novoId(), NivelAlerta.INFO,
                            "Aguardando primeira leitura MQTT do sensor de umidade do solo (cultura: " + cultura + ")"));
                    avisadoAusencia = true;
                }
                logger.warn("Nenhuma leitura MQTT disponivel para cultura {}; ciclo adiado", cultura);
                return;
            }
            avisadoAusencia = false;

            double umidade = ultima.get().getValor();
            Irrigacao resultado = cicloService.executarCiclo(cultura, umidade);
            DadosClimaticos clima = cicloService.getUltimoClima();

            String estrategia = cicloService.getMotorDeRegras().getEstrategiaAtual() != null
                    ? cicloService.getMotorDeRegras().getEstrategiaAtual().getNome()
                    : "Aguardando";

            state.registrarCiclo(umidade, clima, resultado, estrategia);
        } catch (Exception e) {
            logger.error("Erro no ciclo de irrigacao", e);
        }
    }

    private static String novoId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
