package com.irrigacao.ui.web;

import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.dados.NotificadorComposto;
import com.irrigacao.dados.ProcessadorDeDados;
import com.irrigacao.dados.RepositorioDeCulturaEmMemoria;
import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.dados.bd.ConexaoH2;
import com.irrigacao.dados.bd.RepositorioDeIrrigacaoH2;
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
        var conexao = ConexaoH2.emMemoria();
        var repoIrrigacao = new RepositorioDeIrrigacaoH2(conexao.getDataSource());
        return new ServicoDeCicloIrrigacao(
                climaStub, repo, motor, proc, gerenciador, repoIrrigacao, "SP", "BR");
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
        Sensor sensorSolo = new Sensor("SU-milho", TipoSensor.UMIDADE_SOLO, "milho");
        // Cultura ativa default = "milho"; cache deve ter leitura PARA ESSA cultura
        leituras.registrar(sensorSolo, 35.0, LocalDateTime.now(), "milho");

        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        exec.executarCiclo(); // síncrono
        assertNotNull(state.getUltimaIrrigacao(),
                "ciclo deveria ter rodado e registrado irrigacao");
    }

    @Test
    void cicloUsaLeituraDaCulturaAtiva() {
        EstadoDoDashboard state = new EstadoDoDashboard();
        EstadoUltimasLeituras leituras = new EstadoUltimasLeituras();
        Sensor sMilho = new Sensor("SU-milho", TipoSensor.UMIDADE_SOLO, "milho");
        Sensor sSoja  = new Sensor("SU-soja",  TipoSensor.UMIDADE_SOLO, "soja");
        leituras.registrar(sMilho, 22.0, LocalDateTime.now(), "milho"); // baixa
        leituras.registrar(sSoja,  60.0, LocalDateTime.now(), "soja");  // ok

        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        // Cultura ativa default = milho → ciclo usa 22.0 (baixa, vai irrigar)
        exec.executarCiclo();
        double umidadeUsadaMilho = state.getUltimaUmidadeSolo();
        assertEquals(22.0, umidadeUsadaMilho, 0.0001);

        // Troca cultura ativa para soja → proximo ciclo usa 60.0
        state.setCulturaAtiva("soja");
        exec.executarCiclo();
        assertEquals(60.0, state.getUltimaUmidadeSolo(), 0.0001);
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
        Sensor sensorMilho = new Sensor("SU-milho", TipoSensor.UMIDADE_SOLO, "milho");
        ExecutorDeSimulacao exec = new ExecutorDeSimulacao(cicloPadrao(), leituras, state);

        exec.executarCiclo(); // sem leitura → alerta 1
        leituras.registrar(sensorMilho, 40.0, LocalDateTime.now(), "milho");
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
