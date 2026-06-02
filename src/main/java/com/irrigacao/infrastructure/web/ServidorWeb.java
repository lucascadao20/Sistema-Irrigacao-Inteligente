package com.irrigacao.infrastructure.web;

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
import com.irrigacao.application.fabrica.FabricaDeEstrategia;
import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.dados.RepositorioDeCultura;
import com.irrigacao.dados.NotificadorDeAlerta;
import com.irrigacao.application.servico.ServicoDeCicloIrrigacao;
import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.application.servico.MotorDeRegras;
import com.irrigacao.dados.ProcessadorDeDados;
import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.dados.ClienteOpenWeatherMap;
import com.irrigacao.dados.ServicoDeClimaOpenWeatherMap;
import com.irrigacao.dados.NotificadorComposto;
import com.irrigacao.dados.NotificadorDeAlertaLog;
import com.irrigacao.dados.RepositorioDeCulturaEmMemoria;
import com.irrigacao.dados.SimuladorSensores;

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

        ServicoDeCicloIrrigacao cicloService = new ServicoDeCicloIrrigacao(
                servicoDeClima, repositorioDeCultura, motor, processador, gerenciador, cidade, pais);

        ExecutorDeSimulacao executor = new ExecutorDeSimulacao(
                cicloService, simulador, servicoDeClima, state, cidade, pais);
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.parar();
            app.stop();
        }));

        app.start(porta);
        logger.info("Dashboard disponivel em http://localhost:{}", porta);
        System.out.printf("%n[WEB] Dashboard disponivel em http://localhost:%d%n", porta);
        System.out.println("[WEB] Pressione Ctrl+C para encerrar.");
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
