package com.irrigacao.simulador;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irrigacao.dados.mqtt.ConfiguracaoMqtt;
import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AgendadorPublicacaoTest {

    private static final List<String> CULTURAS_TESTE = List.of("milho", "soja", "arroz");

    private static ConfiguracaoMqtt cfgValida() {
        Properties p = new Properties();
        p.setProperty("mqtt.broker.url", "tcp://localhost:1883");
        p.setProperty("mqtt.topico.umidade_solo", "t/{cultura}/umidade_solo");
        p.setProperty("mqtt.topico.temperatura",  "t/global/temperatura");
        p.setProperty("mqtt.topico.umidade_ar",   "t/global/umidade_ar");
        p.setProperty("mqtt.topico.ph_solo",      "t/global/ph_solo");
        return ConfiguracaoMqtt.carregar(p);
    }

    static class PublisherEspia implements MqttPublisher {
        final List<Map.Entry<String, String>> publicacoes = new ArrayList<>();
        @Override public void publicar(String t, String p) { publicacoes.add(Map.entry(t, p)); }
        @Override public void desconectar() { }
    }

    @Test
    void tickPublicaUmaMensagemPorCulturaParaUmidadeDoSolo() {
        PublisherEspia espia = new PublisherEspia();
        AgendadorPublicacao a = new AgendadorPublicacao(
                espia, new GeradorLeituraProgressivo(42L), cfgValida(), CULTURAS_TESTE);

        a.executarTick();

        // 3 culturas para solo + 3 globais (temp, ar, pH) = 6 publicacoes
        assertEquals(6, espia.publicacoes.size());

        Set<String> topicosPublicados = espia.publicacoes.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Topicos por cultura
        assertTrue(topicosPublicados.contains("t/milho/umidade_solo"));
        assertTrue(topicosPublicados.contains("t/soja/umidade_solo"));
        assertTrue(topicosPublicados.contains("t/arroz/umidade_solo"));
        // Topicos globais
        assertTrue(topicosPublicados.contains("t/global/temperatura"));
        assertTrue(topicosPublicados.contains("t/global/umidade_ar"));
        assertTrue(topicosPublicados.contains("t/global/ph_solo"));
    }

    @Test
    void valoresDeUmidadeDoSoloDiferemEntreCulturas() {
        PublisherEspia espia = new PublisherEspia();
        AgendadorPublicacao a = new AgendadorPublicacao(
                espia, new GeradorLeituraProgressivo(42L), cfgValida(), CULTURAS_TESTE);

        a.executarTick();
        Gson gson = new Gson();

        var valoresPorCultura = espia.publicacoes.stream()
                .filter(p -> p.getKey().contains("/umidade_solo"))
                .collect(Collectors.toMap(
                        p -> p.getKey(),
                        p -> gson.fromJson(p.getValue(), JsonObject.class).get("valor").getAsDouble()));

        assertEquals(3, valoresPorCultura.size());
        // Os 3 valores nao podem ser todos iguais (trajetorias independentes).
        long distintos = valoresPorCultura.values().stream().distinct().count();
        assertTrue(distintos >= 2,
                "culturas deveriam ter valores diferentes; valores: " + valoresPorCultura);
    }

    @Test
    void payloadContemTodosOsCamposObrigatorios() {
        PublisherEspia espia = new PublisherEspia();
        AgendadorPublicacao a = new AgendadorPublicacao(
                espia, new GeradorLeituraProgressivo(42L), cfgValida(), CULTURAS_TESTE);

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

    @Test
    void sensorIdDeSoloIncluiNomeDaCultura() {
        PublisherEspia espia = new PublisherEspia();
        AgendadorPublicacao a = new AgendadorPublicacao(
                espia, new GeradorLeituraProgressivo(42L), cfgValida(), List.of("milho"));

        a.executarTick();
        Gson gson = new Gson();

        var soloMilho = espia.publicacoes.stream()
                .filter(p -> p.getKey().equals("t/milho/umidade_solo"))
                .findFirst()
                .orElseThrow();
        JsonObject json = gson.fromJson(soloMilho.getValue(), JsonObject.class);
        assertEquals("SU-milho", json.get("sensorId").getAsString());
    }
}
