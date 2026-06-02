package com.irrigacao.application.servico;

import com.irrigacao.application.interfaces.ServicoDeClima;
import com.irrigacao.application.interfaces.RepositorioDeCultura;
import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.modelo.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ServicoDeCicloIrrigacao {
    private static final Logger logger = LoggerFactory.getLogger(ServicoDeCicloIrrigacao.class);

    private final ServicoDeClima servicoDeClima;
    private final RepositorioDeCultura repositorioDeCultura;
    private final MotorDeRegras motorDeRegras;
    private final ProcessadorDeDados processadorDeDados;
    private final GerenciadorDeSensores gerenciadorDeSensores;
    private final String cidade;
    private final String pais;

    public ServicoDeCicloIrrigacao(ServicoDeClima servicoDeClima, RepositorioDeCultura repositorioDeCultura,
                                  MotorDeRegras motorDeRegras, ProcessadorDeDados processadorDeDados,
                                  GerenciadorDeSensores gerenciadorDeSensores, String cidade, String pais) {
        this.servicoDeClima = servicoDeClima;
        this.repositorioDeCultura = repositorioDeCultura;
        this.motorDeRegras = motorDeRegras;
        this.processadorDeDados = processadorDeDados;
        this.gerenciadorDeSensores = gerenciadorDeSensores;
        this.cidade = cidade;
        this.pais = pais;
    }

    public Irrigacao executarCiclo(String nomeCultura, double umidadeSolo) {
        logger.info("Iniciando ciclo de avaliacao - Cultura: {}", nomeCultura);

        Cultura cultura = repositorioDeCultura.buscarPorNome(nomeCultura)
                .orElseThrow(() -> new IllegalArgumentException("Cultura nao encontrada: " + nomeCultura));

        Sensor sensorUmidade = gerenciadorDeSensores.getSensoresAtivos().stream()
                .filter(s -> s.getTipo() == TipoSensor.UMIDADE_SOLO)
                .findFirst()
                .orElse(null);

        if (sensorUmidade != null) {
            processadorDeDados.processarLeitura(sensorUmidade, umidadeSolo);
        }

        DadosClimaticos clima = servicoDeClima.obterDados(cidade, pais);
        logger.info("Dados climaticos: {}", clima);

        Irrigacao resultado = motorDeRegras.avaliarEExecutar(umidadeSolo, clima, cultura);
        logger.info("Resultado: {}", resultado);

        return resultado;
    }

    public Map<String, Cultura> listarCulturas() {
        return repositorioDeCultura.listarTodas();
    }

    public MotorDeRegras getMotorDeRegras() {
        return motorDeRegras;
    }

    public String getCidade() { return cidade; }
    public String getPais() { return pais; }
}
