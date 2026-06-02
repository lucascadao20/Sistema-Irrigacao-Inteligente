package com.irrigacao.infrastructure.config;

import com.irrigacao.application.fabrica.FabricaDeEstrategia;
import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.dados.RepositorioDeCultura;
import com.irrigacao.dados.NotificadorDeAlerta;
import com.irrigacao.application.servico.ServicoDeCicloIrrigacao;
import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.application.servico.MotorDeRegras;
import com.irrigacao.dados.ProcessadorDeDados;
import com.irrigacao.dados.ClienteOpenWeatherMap;
import com.irrigacao.dados.ServicoDeClimaOpenWeatherMap;
import com.irrigacao.dados.NotificadorComposto;
import com.irrigacao.dados.NotificadorDeAlertaConsole;
import com.irrigacao.dados.NotificadorDeAlertaLog;
import com.irrigacao.dados.RepositorioDeCulturaEmMemoria;
import com.irrigacao.dados.SimuladorSensores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfiguracaoApp {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracaoApp.class);

    private final Properties props;

    public ConfiguracaoApp() {
        this.props = carregarProperties();
    }

    public ServicoDeCicloIrrigacao criarSistema(String nomeAgricultor) {
        String apiKey = props.getProperty("openweathermap.api.key");
        String baseUrl = props.getProperty("openweathermap.base.url");
        String cidade = props.getProperty("cidade.padrao", "Sao Paulo");
        String pais = props.getProperty("cidade.pais", "BR");

        ClienteOpenWeatherMap client = new ClienteOpenWeatherMap(apiKey, baseUrl);
        ServicoDeClima servicoDeClima = new ServicoDeClimaOpenWeatherMap(client);
        RepositorioDeCultura repositorioDeCultura = new RepositorioDeCulturaEmMemoria();

        NotificadorDeAlerta notificador = new NotificadorComposto(
                new NotificadorDeAlertaConsole(nomeAgricultor),
                new NotificadorDeAlertaLog()
        );

        FabricaDeEstrategia fabrica = new FabricaDeEstrategia();
        MotorDeRegras motorDeRegras = new MotorDeRegras(fabrica, notificador);
        GerenciadorDeSensores gerenciador = new GerenciadorDeSensores();
        ProcessadorDeDados processador = new ProcessadorDeDados(gerenciador);

        SimuladorSensores simulador = new SimuladorSensores(gerenciador);
        simulador.inicializarSensores();

        return new ServicoDeCicloIrrigacao(
                servicoDeClima, repositorioDeCultura, motorDeRegras, processador, gerenciador, cidade, pais
        );
    }

    public SimuladorSensores criarSimulador(GerenciadorDeSensores gerenciador) {
        return new SimuladorSensores(gerenciador);
    }

    public RepositorioDeCultura criarRepositorioDeCultura() {
        return new RepositorioDeCulturaEmMemoria();
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
