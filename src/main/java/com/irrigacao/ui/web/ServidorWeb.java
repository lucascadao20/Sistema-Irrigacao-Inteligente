package com.irrigacao.ui.web;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.irrigacao.negocio.FabricaDeEstrategia;
import com.irrigacao.dados.clima.ServicoDeClima;
import com.irrigacao.dados.cultura.RepositorioDeCultura;
import com.irrigacao.dados.notificacao.NotificadorDeAlerta;
import com.irrigacao.negocio.ServicoDeCicloIrrigacao;
import com.irrigacao.dados.sensores.GerenciadorDeSensores;
import com.irrigacao.negocio.MotorDeRegras;
import com.irrigacao.dados.sensores.ProcessadorDeDados;
import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.dados.clima.ClienteOpenWeatherMap;
import com.irrigacao.dados.clima.ServicoDeClimaOpenWeatherMap;
import com.irrigacao.dados.notificacao.NotificadorComposto;
import com.irrigacao.dados.notificacao.NotificadorDeAlertaLog;
import com.irrigacao.dados.cultura.RepositorioDeCulturaEmMemoria;
import com.irrigacao.dados.sensores.SimuladorSensores;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;

public class ServidorWeb {
    private static final Logger logger = LoggerFactory.getLogger(ServidorWeb.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void iniciar(int porta) {
        Properties props = carregarProperties();
        String apiKey = props.getProperty("openweathermap.api.key");
        String baseUrl = props.getProperty("openweathermap.base.url");
        String cidade = props.getProperty("cidade.padrao", "Sao Paulo");
        String pais = props.getProperty("cidade.pais", "BR");

        EstadoDoDashboard state = new EstadoDoDashboard();

        com.irrigacao.dados.mqtt.EstadoUltimasLeituras leituras =
                new com.irrigacao.dados.mqtt.EstadoUltimasLeituras();

        com.irrigacao.dados.mqtt.ConfiguracaoMqtt cfgMqtt = null;
        try {
            cfgMqtt = com.irrigacao.dados.mqtt.ConfiguracaoMqtt.carregar(props);
        } catch (IllegalStateException e) {
            logger.warn("Configuracao MQTT incompleta ({}). Veja config.properties.example. " +
                    "O dashboard rodara sem MQTT e o ciclo aguardara leituras.", e.getMessage());
        }

        ClienteOpenWeatherMap client = new ClienteOpenWeatherMap(apiKey, baseUrl);
        ServicoDeClima servicoDeClima = new ServicoDeClimaOpenWeatherMap(client);
        RepositorioDeCultura repositorioDeCultura = new RepositorioDeCulturaEmMemoria();

        NotificadorDeAlerta notificador = new NotificadorComposto(
                new NotificadorDeAlertaWeb(state),
                new NotificadorDeAlertaLog()
        );

        MotorDeRegras motor = new MotorDeRegras(new FabricaDeEstrategia(), notificador);
        GerenciadorDeSensores gerenciador = new GerenciadorDeSensores();
        ProcessadorDeDados processador = new ProcessadorDeDados(gerenciador);
        SimuladorSensores simulador = new SimuladorSensores(gerenciador);
        simulador.inicializarSensores();

        com.irrigacao.dados.bd.ConexaoH2 conexaoBd =
                com.irrigacao.dados.bd.ConexaoH2.paraArquivo(java.nio.file.Paths.get("data"));
        com.irrigacao.dados.bd.RepositorioDeLeituraSensorH2 repositorioLeitura =
                new com.irrigacao.dados.bd.RepositorioDeLeituraSensorH2(conexaoBd.getDataSource());
        com.irrigacao.dados.bd.RepositorioDeIrrigacaoH2 repositorioIrrigacao =
                new com.irrigacao.dados.bd.RepositorioDeIrrigacaoH2(conexaoBd.getDataSource());

        com.irrigacao.dados.mqtt.ColetorMqttSensores coletor = null;
        if (cfgMqtt != null) {
            coletor = new com.irrigacao.dados.mqtt.ColetorMqttSensores(
                    cfgMqtt, leituras, gerenciador, repositorioLeitura);
            try {
                coletor.iniciar();
            } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
                logger.warn("Nao foi possivel conectar ao broker MQTT em {}: {}. " +
                        "O ciclo ira aguardar leituras.", cfgMqtt.brokerUrl(), e.getMessage());
            }
        }

        ServicoDeCicloIrrigacao cicloService = new ServicoDeCicloIrrigacao(
                servicoDeClima, repositorioDeCultura, motor, processador, gerenciador,
                repositorioIrrigacao, cidade, pais);

        ExecutorDeSimulacao executor = new ExecutorDeSimulacao(cicloService, leituras, state);
        executor.iniciar(30);

        JsonMapper gsonMapper = new JsonMapper() {
            private final Gson gson = new Gson();

            @Override
            public String toJsonString(Object obj, Type type) {
                return gson.toJson(obj, type);
            }

            @Override
            public <T> T fromJsonString(String json, Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(gsonMapper);
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "/static";
                staticFiles.location = Location.CLASSPATH;
            });
        });

        app.get("/api/state", ctx -> ctx.json(montarSnapshot(state)));

        app.get("/api/culturas", ctx -> {
            Map<String, Cultura> culturas = repositorioDeCultura.listarTodas();
            List<Map<String, Object>> lista = culturas.entrySet().stream().map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                Cultura c = e.getValue();
                m.put("id", e.getKey());
                m.put("nome", c.getNome());
                m.put("umidadeMinima", c.getUmidadeMinima());
                m.put("umidadeIdeal", c.getUmidadeIdeal());
                m.put("necessidadeHidrica", c.getNecessidadeHidrica());
                m.put("coeficienteCultura", c.getCoeficienteCultura());
                return m;
            }).toList();
            ctx.json(lista);
        });

        app.post("/api/cultura", ctx -> {
            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String nova = body.get("cultura");
            if (nova != null && repositorioDeCultura.buscarPorNome(nova).isPresent()) {
                state.setCulturaAtiva(nova.toLowerCase());
                ctx.json(Map.of("ok", true, "cultura", state.getCulturaAtiva()));
            } else {
                ctx.status(400).json(Map.of("ok", false, "erro", "cultura invalida"));
            }
        });

        app.get("/api/relatorios/consumo", ctx -> {
            String culturaParam = ctx.queryParam("cultura");
            String inicioParam = ctx.queryParam("inicio");
            String fimParam = ctx.queryParam("fim");

            java.time.LocalDateTime inicio;
            java.time.LocalDateTime fim;
            try {
                inicio = inicioParam != null
                        ? java.time.LocalDateTime.parse(inicioParam)
                        : java.time.LocalDateTime.now().minusDays(7);
                fim = fimParam != null
                        ? java.time.LocalDateTime.parse(fimParam)
                        : java.time.LocalDateTime.now();
            } catch (java.time.format.DateTimeParseException e) {
                ctx.status(400).json(Map.of("ok", false, "erro",
                        "datas devem estar em ISO_LOCAL_DATE_TIME (ex.: 2026-06-21T00:00:00)"));
                return;
            }

            java.util.Optional<String> filtroCultura = (culturaParam != null && !culturaParam.isBlank())
                    ? java.util.Optional.of(resolverNomeCultura(culturaParam, repositorioDeCultura))
                    : java.util.Optional.empty();

            var consumo = repositorioIrrigacao.consumoNoPeriodo(filtroCultura, inicio, fim);
            var lista = repositorioIrrigacao.listar(filtroCultura, inicio, fim);

            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("cultura", consumo.culturaNome());
            resposta.put("inicio", inicio.format(ISO));
            resposta.put("fim", fim.format(ISO));
            resposta.put("consumoTotal", consumo.volumeTotal());
            resposta.put("qtdIrrigacoes", consumo.qtdIrrigacoes());
            resposta.put("irrigacoes", lista.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("decididoEm", r.decididoEm().format(ISO));
                m.put("status", r.status().name());
                m.put("volumeAgua", r.volumeAgua());
                m.put("estrategia", r.estrategiaNome());
                m.put("motivo", r.motivo());
                m.put("umidadeSolo", r.umidadeSolo());
                return m;
            }).toList());
            ctx.json(resposta);
        });

        final com.irrigacao.dados.mqtt.ColetorMqttSensores coletorFinal = coletor;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.parar();
            if (coletorFinal != null) coletorFinal.parar();
            conexaoBd.fechar();
            app.stop();
        }));

        app.start(porta);
        logger.info("Dashboard disponivel em http://localhost:{}", porta);
        System.out.printf("%n[WEB] Dashboard disponivel em http://localhost:%d%n", porta);
        System.out.println("[WEB] Pressione Ctrl+C para encerrar.");
    }

    private String resolverNomeCultura(String entrada, RepositorioDeCultura repo) {
        return repo.buscarPorNome(entrada)
                .map(Cultura::getNome)
                .orElse(entrada);
    }

    private Map<String, Object> montarSnapshot(EstadoDoDashboard state) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("culturaAtiva", state.getCulturaAtiva());
        snapshot.put("umidadeSolo", state.getUltimaUmidadeSolo());
        snapshot.put("estrategia", state.getEstrategiaAtual());
        snapshot.put("atualizadoEm", state.getAtualizadoEm().format(ISO));

        DadosClimaticos clima = state.getUltimoClima();
        if (clima != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("cidade", clima.getCidade());
            c.put("temperatura", clima.getTemperatura());
            c.put("umidadeAr", clima.getUmidadeAr());
            c.put("velocidadeVento", clima.getVelocidadeVento());
            c.put("descricao", clima.getDescricaoClima());
            c.put("previsaoChuva", clima.isPrevisaoChuva());
            c.put("volumeChuva", clima.getVolumeChuva());
            snapshot.put("clima", c);
        }

        Irrigacao ir = state.getUltimaIrrigacao();
        if (ir != null) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("id", ir.getId());
            i.put("status", ir.getStatus().name());
            i.put("statusDescricao", ir.getStatus().getDescricao());
            i.put("volumeAgua", ir.getVolumeAgua());
            i.put("motivo", ir.getMotivo());
            i.put("cultura", ir.getCultura().getNome());
            snapshot.put("irrigacao", i);
        }

        snapshot.put("historico", state.getHistorico().stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timestamp", h.timestamp().format(ISO));
            m.put("umidadeSolo", h.umidadeSolo());
            m.put("volumeAgua", h.volumeAgua());
            m.put("status", h.status());
            return m;
        }).toList());

        snapshot.put("alertas", state.getAlertas().stream().map(this::alertaParaMap).toList());

        return snapshot;
    }

    private Map<String, Object> alertaParaMap(Alerta a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("nivel", a.getNivel().name());
        m.put("mensagem", a.getMensagem());
        m.put("timestamp", a.getTimestamp().format(ISO));
        return m;
    }

    private Properties carregarProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        } catch (IOException e) {
            logger.error("Erro ao carregar config.properties", e);
        }
        return props;
    }
}
