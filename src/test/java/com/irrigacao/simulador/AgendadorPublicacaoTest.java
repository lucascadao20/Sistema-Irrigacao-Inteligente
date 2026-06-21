package com.irrigacao.simulador;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irrigacao.dados.mqtt.ConfiguracaoMqtt;
import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class AgendadorPublicacaoTest {

    private static ConfiguracaoMqtt cfgValida() {
        Properties p = new Properties();
        p.setProperty("mqtt.broker.url", "tcp://localhost:1883");
        p.setProperty("mqtt.topico.umidade_solo", "t/umidade_solo");
        p.setProperty("mqtt.topico.temperatura",  "t/temperatura");
        p.setProperty("mqtt.topico.umidade_ar",   "t/umidade_ar");
        p.setProperty("mqtt.topico.ph_solo",      "t/ph_solo");
        return ConfiguracaoMqtt.carregar(p);
    }

    static class PublisherEspia implements MqttPublisher {
        final List<Map.Entry<String, String>> publicacoes = new ArrayList<>();
        @Override public void publicar(String t, String p) { publicacoes.add(Map.entry(t, p)); }
        @Override public void desconectar() { }
    }

    @Test
    void tickDevePublicarUmaMensagemPorSensorNoTopicoCerto() {
        PublisherEspia espia = new PublisherEspia();
        AgendadorPublicacao a = new AgendadorPublicacao(
                espia, new GeradorLeituraProgressivo(42L), cfgValida());

        a.executarTick();

        assertEquals(4, espia.publicacoes.size());
        Map<TipoSensor, String> esperados = new EnumMap<>(TipoSensor.class);
        esperados.put(TipoSensor.UMIDADE_SOLO, "t/umidade_solo");
        esperados.put(TipoSensor.TEMPERATURA,  "t/temperatura");
        esperados.put(TipoSensor.UMIDADE_AR,   "t/umidade_ar");
        esperados.put(TipoSensor.PH_SOLO,      "t/ph_solo");

        for (Map.Entry<TipoSensor, String> e : esperados.entrySet()) {
            boolean achou = espia.publicacoes.stream()
                    .anyMatch(pub -> pub.getKey().equals(e.getValue()));
            assertTrue(achou, "tópico ausente: " + e.getValue());
        }
    }

    @Test
    void payloadDevePossuirTodosOsCamposObrigatorios() {
        PublisherEspia espia = new PublisherEspia();
        AgendadorPublicacao a = new AgendadorPublicacao(
                espia, new GeradorLeituraProgressivo(42L), cfgValida());

        a.executarTick();
        Gson gson = new Gson();

        for (Map.Entry<String, String> pub : espia.publicacoes) {
            JsonObject json = gson.fromJson(pub.getValue(), JsonObject.class);
            assertTrue(json.has("sensorId"),   "sensorId ausente em " + pub);
            assertTrue(json.has("tipo"),       "tipo ausente em " + pub);
            assertTrue(json.has("valor"),      "valor ausente em " + pub);
            assertTrue(json.has("unidade"),    "unidade ausente em " + pub);
            assertTrue(json.has("timestamp"),  "timestamp ausente em " + pub);
            assertFalse(Double.isNaN(json.get("valor").getAsDouble()));
        }
    }
}
