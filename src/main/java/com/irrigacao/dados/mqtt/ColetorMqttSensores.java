package com.irrigacao.dados.mqtt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.dados.bd.RepositorioDeLeituraSensor;
import com.irrigacao.modelo.LeituraSensor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ColetorMqttSensores {
    private static final Logger logger = LoggerFactory.getLogger(ColetorMqttSensores.class);
    private static final String PLACEHOLDER = "{cultura}";

    /**
     * Cada entrada e um topico assinado: o pattern original (com {cultura} ou
     * fixo), o tipo de sensor associado, o prefixo e o sufixo do pattern para
     * extrair a cultura quando o topic for variavel.
     */
    private record Inscricao(String pattern, TipoSensor tipo, String prefixo, String sufixo, boolean porCultura) {
        static Inscricao de(String pattern, TipoSensor tipo) {
            int idx = pattern.indexOf(PLACEHOLDER);
            if (idx < 0) {
                return new Inscricao(pattern, tipo, "", "", false);
            }
            return new Inscricao(
                    pattern,
                    tipo,
                    pattern.substring(0, idx),
                    pattern.substring(idx + PLACEHOLDER.length()),
                    true);
        }

        String wildcardMqtt() {
            return porCultura ? prefixo + "+" + sufixo : pattern;
        }

        /** Extrai cultura do topic recebido; retorna GLOBAL se a inscricao for fixa. */
        String extrairCultura(String topic) {
            if (!porCultura) return EstadoUltimasLeituras.GLOBAL;
            if (topic.startsWith(prefixo) && topic.endsWith(sufixo)
                    && topic.length() > prefixo.length() + sufixo.length()) {
                return topic.substring(prefixo.length(), topic.length() - sufixo.length());
            }
            return null;
        }
    }

    private final ConfiguracaoMqtt cfg;
    private final EstadoUltimasLeituras estado;
    private final GerenciadorDeSensores gerenciador;
    private final RepositorioDeLeituraSensor repositorioLeitura;
    private final List<Inscricao> inscricoes;

    private MqttClient client;

    public ColetorMqttSensores(ConfiguracaoMqtt cfg,
                                EstadoUltimasLeituras estado,
                                GerenciadorDeSensores gerenciador,
                                RepositorioDeLeituraSensor repositorioLeitura) {
        this.cfg = cfg;
        this.estado = estado;
        this.gerenciador = gerenciador;
        this.repositorioLeitura = repositorioLeitura;
        this.inscricoes = new ArrayList<>();
        cfg.topicosPorTipo().forEach((tipo, pattern) ->
                inscricoes.add(Inscricao.de(pattern, tipo)));
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
        for (Inscricao i : inscricoes) {
            client.subscribe(i.wildcardMqtt(), 0);
        }
        logger.info("ColetorMqttSensores conectado a {} e assinado em {} patterns",
                cfg.brokerUrl(), inscricoes.size());
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
        Inscricao inscricao = casarInscricao(topico);
        if (inscricao == null) {
            logger.warn("Mensagem recebida em topico desconhecido: {}", topico);
            return;
        }

        String cultura = inscricao.extrairCultura(topico);
        if (cultura == null) {
            logger.warn("Nao foi possivel extrair cultura do topico {}", topico);
            return;
        }

        Optional<Double> valorOpt = extrairValor(payload);
        if (valorOpt.isEmpty()) {
            logger.warn("Payload invalido em {}: {}", topico, abreviar(payload));
            return;
        }

        Sensor sensor = encontrarSensor(inscricao.tipo(), cultura);
        if (sensor == null) {
            logger.warn("Nenhum sensor ativo para tipo {} / cultura '{}'",
                    inscricao.tipo(), cultura);
            return;
        }

        // Usamos o instante de recepção em vez do timestamp do payload — para o
        // ciclo de irrigação o que importa é "quão recente é a leitura sob o ponto
        // de vista do app". O timestamp do publicador fica disponível no log do
        // broker para diagnóstico, se necessário.
        LocalDateTime agora = LocalDateTime.now();
        estado.registrar(sensor, valorOpt.get(), agora, cultura);
        repositorioLeitura.salvar(new LeituraSensor(sensor, valorOpt.get(), agora));
    }

    private Inscricao casarInscricao(String topico) {
        for (Inscricao i : inscricoes) {
            if (i.porCultura()) {
                if (topico.startsWith(i.prefixo()) && topico.endsWith(i.sufixo())
                        && topico.length() > i.prefixo().length() + i.sufixo().length()) {
                    return i;
                }
            } else if (topico.equals(i.pattern())) {
                return i;
            }
        }
        return null;
    }

    private Sensor encontrarSensor(TipoSensor tipo, String cultura) {
        if (EstadoUltimasLeituras.GLOBAL.equals(cultura)) {
            return gerenciador.getSensoresAtivos().stream()
                    .filter(s -> s.getTipo() == tipo)
                    .findFirst().orElse(null);
        }
        return gerenciador.getSensoresAtivos().stream()
                .filter(s -> s.getTipo() == tipo && cultura.equals(s.getLocalizacao()))
                .findFirst().orElse(null);
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
