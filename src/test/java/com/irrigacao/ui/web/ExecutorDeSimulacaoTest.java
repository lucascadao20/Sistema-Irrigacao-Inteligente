package com.irrigacao.ui.web;

import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.dados.NotificadorComposto;
import com.irrigacao.dados.ProcessadorDeDados;
import com.irrigacao.dados.RepositorioDeCulturaEmMemoria;
import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.dados.mqtt.EstadoUltimasLeituras;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.negocio.FabricaDeEstrategia;
import com.irrigacao.negocio.MotorDeRegras;
import com.irrigacao.negocio.ServicoDeCicloIrrigacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorDeSimulacaoTest {

    private static DadosClimaticos climaPadrao() {
        return DadosClimaticos.builder()
                .temperatura(25.0).umidadeAr(50.0).velocidadeVento(2.0)
                .descricaoClima("limpo").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();
    }

    private ServicoDeCicloIrrigacao cicloPadrao() {
        ServicoDeClima climaStub = (cidade, pais) -> climaPadrao();
        var repo = new RepositorioDeCulturaEmMemoria();
        var motor = new MotorDeRegras(new FabricaDeEstrategia(),
                new NotificadorComposto());
        var gerenciador = new GerenciadorDeSensores();
        var proc = new ProcessadorDeDados(gerenciador);
        return new ServicoDeCicloIrrigacao(climaStub, repo, motor, proc, gerenciador, "SP", "BR");
    }

    @Test
    void semLeituraDisponivelNaoExecutaCicloEAvisaNoDashboard() {
        EstadoDoDashboard state = new EstadoDoDashboard();
        EstadoUltimasLeituras leituras = new EstadoUltimasLeituras();

        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        exec.executarCiclo(); // chamada síncrona (package-private)
        assertNull(state.getUltimaIrrigacao());
        assertFalse(state.getAlertas().isEmpty(),
                "deveria ter emitido alerta avisando ausencia de leitura");
    }

    @Test
    void comLeituraDisponivelExecutaCicloNormalmente() {
        EstadoDoDashboard state = new EstadoDoDashboard();
        EstadoUltimasLeituras leituras = new EstadoUltimasLeituras();
        Sensor sensorSolo = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A");
        leituras.registrar(sensorSolo, 35.0, LocalDateTime.now());

        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        exec.executarCiclo(); // síncrono
        assertNotNull(state.getUltimaIrrigacao(),
                "ciclo deveria ter rodado e registrado irrigacao");
    }

    @Test
    void multiplosCiclosSemLeituraEmitemApenasUmAlerta() {
        EstadoDoDashboard state = new EstadoDoDashboard();
        EstadoUltimasLeituras leituras = new EstadoUltimasLeituras();
        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        exec.executarCiclo();
        exec.executarCiclo();
        exec.executarCiclo();

        assertEquals(1, state.getAlertas().size(),
                "alerta de ausencia deve ser emitido apenas uma vez consecutiva");
    }

    @Test
    void flagDeAusenciaReiniciaQuandoLeituraChega() {
        EstadoDoDashboard state = new EstadoDoDashboard();
        EstadoUltimasLeituras leituras = new EstadoUltimasLeituras();
        Sensor sensorSolo = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A");
        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        exec.executarCiclo(); // sem leitura → alerta 1
        leituras.registrar(sensorSolo, 40.0, LocalDateTime.now());
        exec.executarCiclo(); // com leitura → roda ciclo, reseta flag

        // Simula broker caiu e cache foi zerado — novo executor (nova instância, flag resetada)
        EstadoUltimasLeituras leiturasVazias = new EstadoUltimasLeituras();
        ExecutorDeSimulacao exec2 = new ExecutorDeSimulacao(cicloPadrao(), leiturasVazias, state);
        exec2.executarCiclo(); // sem leitura de novo → alerta 2

        long ausencias = state.getAlertas().stream()
                .filter(a -> a.getMensagem().contains("Aguardando primeira leitura"))
                .count();
        assertEquals(2, ausencias, "cada periodo de ausencia deve emitir 1 alerta");
    }
}
