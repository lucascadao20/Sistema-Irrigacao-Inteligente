package com.irrigacao.simulador;

import com.irrigacao.dados.mqtt.ConfiguracaoMqtt;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class PublicadorMqtt implements MqttPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PublicadorMqtt.class);

    private final ConfiguracaoMqtt cfg;
    private MqttClient client;

    public PublicadorMqtt(ConfiguracaoMqtt cfg) {
        this.cfg = cfg;
    }

    public void iniciar() throws MqttException {
        client = new MqttClient(cfg.brokerUrl(), cfg.clientIdSimulador(), new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(10);
        client.connect(opts);
        logger.info("PublicadorMqtt conectado a {}", cfg.brokerUrl());
    }

    @Override
    public void publicar(String topico, String payload) {
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(0);
            msg.setRetained(true);
            client.publish(topico, msg);
        } catch (MqttException e) {
            logger.warn("Falha ao publicar em {}: {}", topico, e.getMessage());
        }
    }

    @Override
    public void desconectar() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            logger.warn("Falha ao desconectar publicador: {}", e.getMessage());
        }
    }
}
