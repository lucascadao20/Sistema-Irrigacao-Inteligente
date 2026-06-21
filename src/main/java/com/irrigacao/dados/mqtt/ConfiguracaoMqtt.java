package com.irrigacao.dados.mqtt;

import com.irrigacao.modelo.TipoSensor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public record ConfiguracaoMqtt(
        String brokerUrl,
        String clientIdApp,
        String clientIdSimulador,
        int intervaloPublicacaoSegundos,
        Map<TipoSensor, String> topicosPorTipo
) {

    public static ConfiguracaoMqtt carregar(Properties props) {
        String broker = exigir(props, "mqtt.broker.url");
        String idApp = props.getProperty("mqtt.client.id.app", "irrigacao-app");
        String idSim = props.getProperty("mqtt.client.id.simulador", "irrigacao-simulador");
        int intervalo = Integer.parseInt(props.getProperty("mqtt.intervalo.publicacao.segundos", "5"));

        Map<TipoSensor, String> topicos = new EnumMap<>(TipoSensor.class);
        topicos.put(TipoSensor.UMIDADE_SOLO, exigir(props, "mqtt.topico.umidade_solo"));
        topicos.put(TipoSensor.TEMPERATURA,  exigir(props, "mqtt.topico.temperatura"));
        topicos.put(TipoSensor.UMIDADE_AR,   exigir(props, "mqtt.topico.umidade_ar"));
        topicos.put(TipoSensor.PH_SOLO,      exigir(props, "mqtt.topico.ph_solo"));

        return new ConfiguracaoMqtt(broker, idApp, idSim, intervalo, Map.copyOf(topicos));
    }

    private static String exigir(Properties p, String chave) {
        String v = p.getProperty(chave);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Propriedade ausente: " + chave);
        }
        return v;
    }
}
