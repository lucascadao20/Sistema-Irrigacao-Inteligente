package com.irrigacao.infrastructure.config;

import com.irrigacao.application.factory.StrategyFactory;
import com.irrigacao.application.port.ClimaService;
import com.irrigacao.application.port.CulturaRepository;
import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.application.service.CicloIrrigacaoService;
import com.irrigacao.application.service.GerenciadorSensores;
import com.irrigacao.application.service.MotorRegras;
import com.irrigacao.application.service.ProcessadorDados;
import com.irrigacao.infrastructure.api.OpenWeatherMapClient;
import com.irrigacao.infrastructure.api.OpenWeatherMapClimaService;
import com.irrigacao.infrastructure.notification.CompositeNotificador;
import com.irrigacao.infrastructure.notification.ConsoleNotificadorAlerta;
import com.irrigacao.infrastructure.notification.LogNotificadorAlerta;
import com.irrigacao.infrastructure.persistence.InMemoryCulturaRepository;
import com.irrigacao.infrastructure.simulator.SimuladorSensores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private final Properties props;

    public AppConfig() {
        this.props = carregarProperties();
    }

    public CicloIrrigacaoService criarSistema(String nomeAgricultor) {
        String apiKey = props.getProperty("openweathermap.api.key");
        String baseUrl = props.getProperty("openweathermap.base.url");
        String cidade = props.getProperty("cidade.padrao", "Sao Paulo");
        String pais = props.getProperty("cidade.pais", "BR");

        OpenWeatherMapClient client = new OpenWeatherMapClient(apiKey, baseUrl);
        ClimaService climaService = new OpenWeatherMapClimaService(client);
        CulturaRepository culturaRepo = new InMemoryCulturaRepository();

        NotificadorAlerta notificador = new CompositeNotificador(
                new ConsoleNotificadorAlerta(nomeAgricultor),
                new LogNotificadorAlerta()
        );

        StrategyFactory factory = new StrategyFactory();
        MotorRegras motorRegras = new MotorRegras(factory, notificador);
        GerenciadorSensores gerenciador = new GerenciadorSensores();
        ProcessadorDados processador = new ProcessadorDados(gerenciador);

        SimuladorSensores simulador = new SimuladorSensores(gerenciador);
        simulador.inicializarSensores();

        return new CicloIrrigacaoService(
                climaService, culturaRepo, motorRegras, processador, gerenciador, cidade, pais
        );
    }

    public SimuladorSensores criarSimulador(GerenciadorSensores gerenciador) {
        return new SimuladorSensores(gerenciador);
    }

    public CulturaRepository criarCulturaRepository() {
        return new InMemoryCulturaRepository();
    }

    public String getCidade() {
        return props.getProperty("cidade.padrao", "Sao Paulo");
    }

    public String getPais() {
        return props.getProperty("cidade.pais", "BR");
    }

    private Properties carregarProperties() {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                properties.load(is);
                logger.info("Configuracoes carregadas de config.properties");
            } else {
                logger.warn("config.properties nao encontrado. Usando valores padrao.");
            }
        } catch (IOException e) {
            logger.error("Erro ao carregar configuracoes: {}", e.getMessage());
        }
        return properties;
    }
}
