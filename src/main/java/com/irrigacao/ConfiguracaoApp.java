package com.irrigacao;

import com.irrigacao.negocio.FabricaDeEstrategia;
import com.irrigacao.dados.clima.ServicoDeClima;
import com.irrigacao.dados.cultura.RepositorioDeCultura;
import com.irrigacao.dados.notificacao.NotificadorDeAlerta;
import com.irrigacao.negocio.ServicoDeCicloIrrigacao;
import com.irrigacao.dados.sensores.GerenciadorDeSensores;
import com.irrigacao.negocio.MotorDeRegras;
import com.irrigacao.dados.sensores.ProcessadorDeDados;
import com.irrigacao.dados.clima.ClienteOpenWeatherMap;
import com.irrigacao.dados.clima.ServicoDeClimaOpenWeatherMap;
import com.irrigacao.dados.notificacao.NotificadorComposto;
import com.irrigacao.dados.notificacao.NotificadorDeAlertaConsole;
import com.irrigacao.dados.notificacao.NotificadorDeAlertaLog;
import com.irrigacao.dados.cultura.RepositorioDeCulturaEmMemoria;
import com.irrigacao.dados.sensores.SimuladorSensores;
import com.irrigacao.dados.bd.ConexaoH2;
import com.irrigacao.dados.bd.RepositorioDeIrrigacao;
import com.irrigacao.dados.bd.RepositorioDeIrrigacaoH2;
import com.irrigacao.dados.bd.RepositorioDeLeituraSensor;
import com.irrigacao.dados.bd.RepositorioDeLeituraSensorH2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfiguracaoApp {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracaoApp.class);

    private final Properties props;
    private ConexaoH2 conexaoBd;
    private RepositorioDeLeituraSensor repositorioLeitura;
    private RepositorioDeIrrigacao repositorioIrrigacao;

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

        this.conexaoBd = ConexaoH2.paraArquivo(Paths.get("data"));
        this.repositorioLeitura = new RepositorioDeLeituraSensorH2(conexaoBd.getDataSource());
        this.repositorioIrrigacao = new RepositorioDeIrrigacaoH2(conexaoBd.getDataSource());

        return new ServicoDeCicloIrrigacao(
                servicoDeClima, repositorioDeCultura, motorDeRegras, processador, gerenciador,
                repositorioIrrigacao, cidade, pais
        );
    }

    public ConexaoH2 getConexaoBd() { return conexaoBd; }
    public RepositorioDeLeituraSensor getRepositorioLeitura() { return repositorioLeitura; }
    public RepositorioDeIrrigacao getRepositorioIrrigacao() { return repositorioIrrigacao; }

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
