package com.irrigacao.dados.mqtt;

import com.irrigacao.dados.sensores.GerenciadorDeSensores;
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
        p.setProperty("mqtt.topico.umidade_solo", "t/{cultura}/umidade_solo");
        p.setProperty("mqtt.topico.temperatura",  "t/global/temperatura");
        p.setProperty("mqtt.topico.umidade_ar",   "t/global/umidade_ar");
        p.setProperty("mqtt.topico.ph_solo",      "t/global/ph_solo");
        ConfiguracaoMqtt cfg = ConfiguracaoMqtt.carregar(p);

        conexao = ConexaoH2.emMemoria();
        repositorio = new RepositorioDeLeituraSensorH2(conexao.getDataSource());

        estado = new EstadoUltimasLeituras();
        gerenciador = new GerenciadorDeSensores();
        // Sensores de solo por cultura — localizacao = nome da cultura
        gerenciador.registrarSensor(new Sensor("SU-milho", TipoSensor.UMIDADE_SOLO, "milho"));
        gerenciador.registrarSensor(new Sensor("SU-soja",  TipoSensor.UMIDADE_SOLO, "soja"));
        // Sensores globais
        gerenciador.registrarSensor(new Sensor("ST-001", TipoSensor.TEMPERATURA, "global"));
        gerenciador.registrarSensor(new Sensor("SA-001", TipoSensor.UMIDADE_AR,  "global"));
        gerenciador.registrarSensor(new Sensor("SP-001", TipoSensor.PH_SOLO,     "global"));

        coletor = new ColetorMqttSensores(cfg, estado, gerenciador, repositorio);
    }

    @AfterEach
    void teardown() {
        conexao.fechar();
    }

    @Test
    void payloadDeSoloDoMilhoVaiParaCachePorCultura() {
        String payload = "{\"sensorId\":\"SU-milho\",\"tipo\":\"UMIDADE_SOLO\","
                + "\"valor\":42.7,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        coletor.processarMensagem("t/milho/umidade_solo", payload);

        assertEquals(42.7,
                estado.getUltima(TipoSensor.UMIDADE_SOLO, "milho").orElseThrow().getValor(),
                0.0001);
        // Nao popula o cache global
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO).isEmpty());
    }

    @Test
    void leiturasDeCulturasDistintasSaoIndependentes() {
        String pMilho = "{\"sensorId\":\"SU-milho\",\"tipo\":\"UMIDADE_SOLO\","
                + "\"valor\":40.0,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";
        String pSoja  = "{\"sensorId\":\"SU-soja\",\"tipo\":\"UMIDADE_SOLO\","
                + "\"valor\":75.0,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        coletor.processarMensagem("t/milho/umidade_solo", pMilho);
        coletor.processarMensagem("t/soja/umidade_solo",  pSoja);

        assertEquals(40.0, estado.getUltima(TipoSensor.UMIDADE_SOLO, "milho").orElseThrow().getValor());
        assertEquals(75.0, estado.getUltima(TipoSensor.UMIDADE_SOLO, "soja").orElseThrow().getValor());
    }

    @Test
    void leituraGlobalDeTemperaturaPopulaCacheGlobal() {
        String payload = "{\"sensorId\":\"ST-001\",\"tipo\":\"TEMPERATURA\","
                + "\"valor\":24.5,\"unidade\":\"C\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        coletor.processarMensagem("t/global/temperatura", payload);

        assertEquals(24.5,
                estado.getUltima(TipoSensor.TEMPERATURA).orElseThrow().getValor());
    }

    @Test
    void payloadComJsonInvalidoNaoLancaNemPersiste() {
        coletor.processarMensagem("t/milho/umidade_solo", "isto-nao-e-json");
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO, "milho").isEmpty());
        assertEquals(0, repositorio.contar());
    }

    @Test
    void payloadSemCampoValorNaoPersiste() {
        String payload = "{\"sensorId\":\"SU-milho\",\"tipo\":\"UMIDADE_SOLO\"}";
        coletor.processarMensagem("t/milho/umidade_solo", payload);
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO, "milho").isEmpty());
        assertEquals(0, repositorio.contar());
    }

    @Test
    void topicoDesconhecidoEhIgnorado() {
        coletor.processarMensagem("t/random/algumacoisa",
                "{\"valor\":1.0}");
        assertEquals(0, repositorio.contar());
    }

    @Test
    void payloadValidoTambemPersisteNoBanco() {
        String payload = "{\"sensorId\":\"SU-milho\",\"tipo\":\"UMIDADE_SOLO\","
                + "\"valor\":42.7,\"unidade\":\"%\",\"timestamp\":\"2026-06-21T10:00:00\"}";

        assertEquals(0, repositorio.contar());
        coletor.processarMensagem("t/milho/umidade_solo", payload);
        assertEquals(1, repositorio.contar());
    }
}
