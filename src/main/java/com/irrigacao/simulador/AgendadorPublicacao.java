package com.irrigacao.simulador;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irrigacao.dados.mqtt.ConfiguracaoMqtt;
import com.irrigacao.modelo.TipoSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgendadorPublicacao {
    private static final Logger logger = LoggerFactory.getLogger(AgendadorPublicacao.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Gson GSON = new Gson();

    private static final Map<TipoSensor, String> ID_POR_TIPO = Map.of(
            TipoSensor.UMIDADE_SOLO, "SU-001",
            TipoSensor.TEMPERATURA,  "ST-001",
            TipoSensor.UMIDADE_AR,   "SA-001",
            TipoSensor.PH_SOLO,      "SP-001"
    );

    private final MqttPublisher publisher;
    private final GeradorLeituraProgressivo gerador;
    private final ConfiguracaoMqtt cfg;
    private final ScheduledExecutorService executor;

    public AgendadorPublicacao(MqttPublisher publisher,
                                GeradorLeituraProgressivo gerador,
                                ConfiguracaoMqtt cfg) {
        this.publisher = publisher;
        this.gerador = gerador;
        this.cfg = cfg;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "publisher-simulador");
            t.setDaemon(true);
            return t;
        });
    }

    public void iniciar() {
        int intervalo = cfg.intervaloPublicacaoSegundos();
        executor.scheduleAtFixedRate(this::executarTick, 0, intervalo, TimeUnit.SECONDS);
        logger.info("AgendadorPublicacao iniciado com intervalo de {}s", intervalo);
    }

    public void parar() {
        executor.shutdownNow();
        publisher.desconectar();
    }

    public void executarTick() {
        LocalDateTime agora = LocalDateTime.now();
        for (Map.Entry<TipoSensor, String> entry : cfg.topicosPorTipo().entrySet()) {
            TipoSensor tipo = entry.getKey();
            String topico = entry.getValue();
            double valor = gerador.proximaLeitura(tipo);
            String payload = montarPayload(tipo, valor, agora);
            publisher.publicar(topico, payload);
        }
    }

    private static String montarPayload(TipoSensor tipo, double valor, LocalDateTime ts) {
        JsonObject obj = new JsonObject();
        obj.addProperty("sensorId",  ID_POR_TIPO.get(tipo));
        obj.addProperty("tipo",      tipo.name());
        obj.addProperty("valor",     valor);
        obj.addProperty("unidade",   tipo.getUnidade());
        obj.addProperty("timestamp", ts.format(ISO));
        return GSON.toJson(obj);
    }
}
