package com.irrigacao.dados.mqtt;

import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.dados.bd.ConexaoH2;
import com.irrigacao.dados.bd.RepositorioDeLeituraSensorH2;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ColetorMqttSensoresTest {

    private ConexaoH2 conexao;
    private EstadoUltimasLeituras estado;
    private GerenciadorDeSensores gerenciador;
    private RepositorioDeLeituraSensorH2 repositorio;
    private ColetorMqttSensores coletor;

    @BeforeEach
    void setup() {
        Properties p = new Properties();
        p.setProperty("mqtt.broker.url", "tcp://localhost:1883");
        p.setProperty("mqtt.topico.umidade_solo", "t/umidade_solo");
        p.setProperty("mqtt.topico.temperatura",  "t/temperatura");
        p.setProperty("mqtt.topico.umidade_ar",   "t/umidade_ar");
        p.setProperty("mqtt.topico.ph_solo",      "t/ph_solo");
        ConfiguracaoMqtt cfg = ConfiguracaoMqtt.carregar(p);

        conexao = ConexaoH2.emMemoria();
        repositorio = new RepositorioDeLeituraSensorH2(conexao.getDataSource());

        estado = new EstadoUltimasLeituras();
        gerenciador = new GerenciadorDeSensores();
        gerenciador.registrarSensor(new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A"));
        gerenciador.registrarSensor(new Sensor("ST-001", TipoSensor.TEMPERATURA,  "Talhao A"));
        gerenciador.registrarSensor(new Sensor("SA-001", TipoSensor.UMIDADE_AR,   "Talhao A"));
        gerenciador.registrarSensor(new Sensor("SP-001", TipoSensor.PH_SOLO,      "Talhao A"));

        coletor = new ColetorMqttSensores(cfg, estado, gerenciador, repositorio);
    }

    @AfterEach
    void teardown() {
        conexao.fechar();
    }

    @Test
    void payloadValidoAtualizaCache() {
        String payload = "{\"sensorId\":\"SU-001\",\"tipo\":\"UMIDADE_SOLO\","
                + "\"valor\":42.7,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        coletor.processarMensagem("t/umidade_solo", payload);

        assertEquals(42.7,
                estado.getUltima(TipoSensor.UMIDADE_SOLO).orElseThrow().getValor(),
                0.0001);
    }

    @Test
    void payloadValidoTambemPersisteNoBanco() {
        String payload = "{\"sensorId\":\"SU-001\",\"tipo\":\"UMIDADE_SOLO\","
                + "\"valor\":42.7,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        assertEquals(0, repositorio.contar());
        coletor.processarMensagem("t/umidade_solo", payload);
        assertEquals(1, repositorio.contar());
    }

    @Test
    void payloadInvalidoNaoPersisteNoBanco() {
        coletor.processarMensagem("t/umidade_solo", "isto-nao-e-json");
        coletor.processarMensagem("t/umidade_solo", "{\"sensorId\":\"SU-001\"}"); // sem valor
        assertEquals(0, repositorio.contar());
    }

    @Test
    void payloadComJsonInvalidoNaoLancaNemAlteraCache() {
        coletor.processarMensagem("t/umidade_solo", "isto-nao-e-json");
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO).isEmpty());
    }

    @Test
    void payloadSemCampoValorNaoAlteraCache() {
        String payload = "{\"sensorId\":\"SU-001\",\"tipo\":\"UMIDADE_SOLO\"}";
        coletor.processarMensagem("t/umidade_solo", payload);
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO).isEmpty());
    }

    @Test
    void tipoDoTopicoPrecedeOTipoDoPayload() {
        // Se o JSON declara UMIDADE_AR mas chega no topico de UMIDADE_SOLO,
        // o sensor usado é o de UMIDADE_SOLO (a verdade vem do topico).
        String payload = "{\"sensorId\":\"SA-001\",\"tipo\":\"UMIDADE_AR\","
                + "\"valor\":80.0,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        coletor.processarMensagem("t/umidade_solo", payload);

        assertEquals(80.0,
                estado.getUltima(TipoSensor.UMIDADE_SOLO).orElseThrow().getValor(), 0.0001);
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_AR).isEmpty());
    }
}
