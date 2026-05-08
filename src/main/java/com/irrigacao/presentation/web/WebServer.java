package com.irrigacao.presentation.web;

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
import com.irrigacao.application.factory.StrategyFactory;
import com.irrigacao.application.port.ClimaService;
import com.irrigacao.application.port.CulturaRepository;
import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.application.service.CicloIrrigacaoService;
import com.irrigacao.application.service.GerenciadorSensores;
import com.irrigacao.application.service.MotorRegras;
import com.irrigacao.application.service.ProcessadorDados;
import com.irrigacao.domain.model.Alerta;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import com.irrigacao.infrastructure.api.OpenWeatherMapClient;
import com.irrigacao.infrastructure.api.OpenWeatherMapClimaService;
import com.irrigacao.infrastructure.notification.CompositeNotificador;
import com.irrigacao.infrastructure.notification.LogNotificadorAlerta;
import com.irrigacao.infrastructure.persistence.InMemoryCulturaRepository;
import com.irrigacao.infrastructure.simulator.SimuladorSensores;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;

public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void iniciar(int porta) {
        Properties props = carregarProperties();
        String apiKey = props.getProperty("openweathermap.api.key");
        String baseUrl = props.getProperty("openweathermap.base.url");
        String cidade = props.getProperty("cidade.padrao", "Sao Paulo");
        String pais = props.getProperty("cidade.pais", "BR");

        DashboardState state = new DashboardState();

        OpenWeatherMapClient client = new OpenWeatherMapClient(apiKey, baseUrl);
        ClimaService climaService = new OpenWeatherMapClimaService(client);
        CulturaRepository culturaRepo = new InMemoryCulturaRepository();

        NotificadorAlerta notificador = new CompositeNotificador(
                new WebNotificadorAlerta(state),
                new LogNotificadorAlerta()
        );

        MotorRegras motor = new MotorRegras(new StrategyFactory(), notificador);
        GerenciadorSensores gerenciador = new GerenciadorSensores();
        ProcessadorDados processador = new ProcessadorDados(gerenciador);
        SimuladorSensores simulador = new SimuladorSensores(gerenciador);
        simulador.inicializarSensores();

        CicloIrrigacaoService cicloService = new CicloIrrigacaoService(
                climaService, culturaRepo, motor, processador, gerenciador, cidade, pais);

        SimulationRunner runner = new SimulationRunner(
                cicloService, simulador, climaService, state, cidade, pais);
        runner.iniciar(5);

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
            Map<String, Cultura> culturas = culturaRepo.listarTodas();
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
            if (nova != null && culturaRepo.buscarPorNome(nova).isPresent()) {
                state.setCulturaAtiva(nova.toLowerCase());
                ctx.json(Map.of("ok", true, "cultura", state.getCulturaAtiva()));
            } else {
                ctx.status(400).json(Map.of("ok", false, "erro", "cultura invalida"));
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            runner.parar();
            app.stop();
        }));

        app.start(porta);
        logger.info("Dashboard disponivel em http://localhost:{}", porta);
        System.out.printf("%n[WEB] Dashboard disponivel em http://localhost:%d%n", porta);
        System.out.println("[WEB] Pressione Ctrl+C para encerrar.");
    }

    private Map<String, Object> montarSnapshot(DashboardState state) {
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
