package com.irrigacao.dados.mqtt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ColetorMqttSensores {
    private static final Logger logger = LoggerFactory.getLogger(ColetorMqttSensores.class);

    private final ConfiguracaoMqtt cfg;
    private final EstadoUltimasLeituras estado;
    private final GerenciadorDeSensores gerenciador;
    private final Map<String, TipoSensor> tipoPorTopico;

    private MqttClient client;

    public ColetorMqttSensores(ConfiguracaoMqtt cfg,
                                EstadoUltimasLeituras estado,
                                GerenciadorDeSensores gerenciador) {
        this.cfg = cfg;
        this.estado = estado;
        this.gerenciador = gerenciador;
        this.tipoPorTopico = new HashMap<>();
        cfg.topicosPorTipo().forEach((tipo, topico) -> tipoPorTopico.put(topico, tipo));
    }

    public void iniciar() throws MqttException {
        client = new MqttClient(cfg.brokerUrl(), cfg.clientIdApp(), new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(10);

        client.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallback() {
            @Override public void connectionLost(Throwable cause) {
                logger.warn("Conexao MQTT perdida: {}", cause.getMessage());
            }
            @Override
            public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                processarMensagem(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
            }
            @Override public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) { }
        });

        client.connect(opts);
        for (String topico : cfg.topicosPorTipo().values()) {
            client.subscribe(topico, 0);
        }
        logger.info("ColetorMqttSensores conectado a {} e assinado nos {} topicos",
                cfg.brokerUrl(), cfg.topicosPorTipo().size());
    }

    public void parar() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            logger.warn("Falha ao desconectar coletor: {}", e.getMessage());
        }
    }

    public void processarMensagem(String topico, String payload) {
        TipoSensor tipo = tipoPorTopico.get(topico);
        if (tipo == null) {
            logger.warn("Mensagem recebida em topico desconhecido: {}", topico);
            return;
        }

        Optional<Double> valorOpt = extrairValor(payload);
        if (valorOpt.isEmpty()) {
            logger.warn("Payload invalido em {}: {}", topico, abreviar(payload));
            return;
        }

        Sensor sensor = gerenciador.getSensoresAtivos().stream()
                .filter(s -> s.getTipo() == tipo)
                .findFirst()
                .orElse(null);

        if (sensor == null) {
            logger.warn("Nenhum sensor ativo registrado para tipo {}", tipo);
            return;
        }

        estado.registrar(sensor, valorOpt.get(), LocalDateTime.now());
    }

    private static Optional<Double> extrairValor(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if (!json.has("valor")) return Optional.empty();
            return Optional.of(json.get("valor").getAsDouble());
        } catch (JsonSyntaxException | IllegalStateException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String abreviar(String s) {
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }
}
