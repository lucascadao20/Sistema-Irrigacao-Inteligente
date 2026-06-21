package com.irrigacao.dados.mqtt;

import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracaoMqttTest {

    @Test
    void deveCarregarTodosOsCamposDePropertiesValidas() {
        Properties p = new Properties();
        p.setProperty("mqtt.broker.url", "tcp://localhost:1883");
        p.setProperty("mqtt.client.id.app", "irrigacao-app");
        p.setProperty("mqtt.client.id.simulador", "irrigacao-simulador");
        p.setProperty("mqtt.intervalo.publicacao.segundos", "5");
        p.setProperty("mqtt.topico.umidade_solo", "irrigacao/sensores/SU-001/umidade_solo");
        p.setProperty("mqtt.topico.temperatura", "irrigacao/sensores/ST-001/temperatura");
        p.setProperty("mqtt.topico.umidade_ar", "irrigacao/sensores/SA-001/umidade_ar");
        p.setProperty("mqtt.topico.ph_solo", "irrigacao/sensores/SP-001/ph_solo");

        ConfiguracaoMqtt cfg = ConfiguracaoMqtt.carregar(p);

        assertEquals("tcp://localhost:1883", cfg.brokerUrl());
        assertEquals("irrigacao-app", cfg.clientIdApp());
        assertEquals("irrigacao-simulador", cfg.clientIdSimulador());
        assertEquals(5, cfg.intervaloPublicacaoSegundos());
        assertEquals("irrigacao/sensores/SU-001/umidade_solo",
                cfg.topicosPorTipo().get(TipoSensor.UMIDADE_SOLO));
        assertEquals("irrigacao/sensores/ST-001/temperatura",
                cfg.topicosPorTipo().get(TipoSensor.TEMPERATURA));
        assertEquals("irrigacao/sensores/SA-001/umidade_ar",
                cfg.topicosPorTipo().get(TipoSensor.UMIDADE_AR));
        assertEquals("irrigacao/sensores/SP-001/ph_solo",
                cfg.topicosPorTipo().get(TipoSensor.PH_SOLO));
    }

    @Test
    void deveLancarSeFaltarBrokerUrl() {
        Properties p = new Properties();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ConfiguracaoMqtt.carregar(p));
        assertTrue(ex.getMessage().contains("mqtt.broker.url"));
    }
}
