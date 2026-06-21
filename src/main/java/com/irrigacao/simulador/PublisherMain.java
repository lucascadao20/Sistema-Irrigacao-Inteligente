package com.irrigacao.simulador;

import com.irrigacao.dados.mqtt.ConfiguracaoMqtt;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PublisherMain {
    private static final Logger logger = LoggerFactory.getLogger(PublisherMain.class);

    public static void main(String[] args) {
        Properties props = carregarProperties();
        ConfiguracaoMqtt cfg = ConfiguracaoMqtt.carregar(props);

        PublicadorMqtt publisher = new PublicadorMqtt(cfg);
        try {
            publisher.iniciar();
        } catch (MqttException e) {
            logger.error("Nao foi possivel conectar ao broker {}: {}", cfg.brokerUrl(), e.getMessage());
            System.exit(1);
        }

        GeradorLeituraProgressivo gerador = new GeradorLeituraProgressivo();
        AgendadorPublicacao agendador = new AgendadorPublicacao(publisher, gerador, cfg);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Encerrando publicador...");
            agendador.parar();
        }, "publisher-shutdown"));

        agendador.iniciar();
        logger.info("Publicador rodando. Ctrl+C para encerrar.");

        // Mantem o processo vivo
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static Properties carregarProperties() {
        Properties props = new Properties();
        try (InputStream is = PublisherMain.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                logger.warn("config.properties nao encontrado no classpath. Usando apenas defaults.");
            }
        } catch (IOException e) {
            logger.error("Erro ao carregar config.properties: {}", e.getMessage());
        }
        return props;
    }
}
