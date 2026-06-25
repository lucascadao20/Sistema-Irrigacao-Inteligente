package com.irrigacao.simulador;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irrigacao.dados.mqtt.ConfiguracaoMqtt;
import com.irrigacao.modelo.TipoSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgendadorPublicacao {
    private static final Logger logger = LoggerFactory.getLogger(AgendadorPublicacao.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Gson GSON = new Gson();

    /** Placeholder no topico de umidade do solo, substituido por nome da cultura. */
    public static final String PLACEHOLDER_CULTURA = "{cultura}";

    private static final Map<TipoSensor, String> ID_GLOBAL_POR_TIPO = Map.of(
            TipoSensor.TEMPERATURA, "ST-001",
            TipoSensor.UMIDADE_AR,  "SA-001",
            TipoSensor.PH_SOLO,     "SP-001"
    );

    private final MqttPublisher publisher;
    private final GeradorLeituraProgressivo gerador;
    private final ConfiguracaoMqtt cfg;
    private final List<String> culturas;
    private final ScheduledExecutorService executor;

    public AgendadorPublicacao(MqttPublisher publisher,
                                GeradorLeituraProgressivo gerador,
                                ConfiguracaoMqtt cfg,
                                List<String> culturas) {
        this.publisher = publisher;
        this.gerador = gerador;
        this.cfg = cfg;
        this.culturas = List.copyOf(culturas);
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "publisher-simulador");
            t.setDaemon(true);
            return t;
        });
    }

    public void iniciar() {
        int intervalo = cfg.intervaloPublicacaoSegundos();
        executor.scheduleAtFixedRate(this::executarTick, 0, intervalo, TimeUnit.SECONDS);
        logger.info("AgendadorPublicacao iniciado com intervalo de {}s, {} culturas",
                intervalo, culturas.size());
    }

    public void parar() {
        executor.shutdownNow();
        publisher.desconectar();
    }

    public void executarTick() {
        LocalDateTime agora = LocalDateTime.now();

        // Umidade do solo: uma publicacao por cultura
        String topicoSoloPattern = cfg.topicosPorTipo().get(TipoSensor.UMIDADE_SOLO);
        for (String cultura : culturas) {
            double valor = gerador.proximaLeituraSolo(cultura);
            String topico = topicoSoloPattern.replace(PLACEHOLDER_CULTURA, cultura);
            String sensorId = "SU-" + cultura;
            String payload = montarPayload(TipoSensor.UMIDADE_SOLO, valor, agora, sensorId);
            publisher.publicar(topico, payload);
        }

        // Demais sensores: publicacao global (uma por tipo)
        for (Map.Entry<TipoSensor, String> entry : cfg.topicosPorTipo().entrySet()) {
            TipoSensor tipo = entry.getKey();
            if (tipo == TipoSensor.UMIDADE_SOLO) continue;
            String topico = entry.getValue();
            double valor = gerador.proximaLeitura(tipo);
            String payload = montarPayload(tipo, valor, agora, ID_GLOBAL_POR_TIPO.get(tipo));
            publisher.publicar(topico, payload);
        }
    }

    private static String montarPayload(TipoSensor tipo, double valor, LocalDateTime ts, String sensorId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("sensorId",  sensorId);
        obj.addProperty("tipo",      tipo.name());
        obj.addProperty("valor",     valor);
        obj.addProperty("unidade",   tipo.getUnidade());
        obj.addProperty("timestamp", ts.format(ISO));
        return GSON.toJson(obj);
    }
}
