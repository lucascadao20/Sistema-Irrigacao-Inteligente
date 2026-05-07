package com.irrigacao.application.service;

import com.irrigacao.application.port.ClimaService;
import com.irrigacao.application.port.CulturaRepository;
import com.irrigacao.domain.enums.TipoSensor;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import com.irrigacao.domain.model.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CicloIrrigacaoService {
    private static final Logger logger = LoggerFactory.getLogger(CicloIrrigacaoService.class);

    private final ClimaService climaService;
    private final CulturaRepository culturaRepository;
    private final MotorRegras motorRegras;
    private final ProcessadorDados processadorDados;
    private final GerenciadorSensores gerenciadorSensores;
    private final String cidade;
    private final String pais;

    public CicloIrrigacaoService(ClimaService climaService, CulturaRepository culturaRepository,
                                  MotorRegras motorRegras, ProcessadorDados processadorDados,
                                  GerenciadorSensores gerenciadorSensores, String cidade, String pais) {
        this.climaService = climaService;
        this.culturaRepository = culturaRepository;
        this.motorRegras = motorRegras;
        this.processadorDados = processadorDados;
        this.gerenciadorSensores = gerenciadorSensores;
        this.cidade = cidade;
        this.pais = pais;
    }

    public Irrigacao executarCiclo(String nomeCultura, double umidadeSolo) {
        logger.info("Iniciando ciclo de avaliacao - Cultura: {}", nomeCultura);

        Cultura cultura = culturaRepository.buscarPorNome(nomeCultura)
                .orElseThrow(() -> new IllegalArgumentException("Cultura nao encontrada: " + nomeCultura));

        Sensor sensorUmidade = gerenciadorSensores.getSensoresAtivos().stream()
                .filter(s -> s.getTipo() == TipoSensor.UMIDADE_SOLO)
                .findFirst()
                .orElse(null);

        if (sensorUmidade != null) {
            processadorDados.processarLeitura(sensorUmidade, umidadeSolo);
        }

        DadosClimaticos clima = climaService.obterDados(cidade, pais);
        logger.info("Dados climaticos: {}", clima);

        Irrigacao resultado = motorRegras.avaliarEExecutar(umidadeSolo, clima, cultura);
        logger.info("Resultado: {}", resultado);

        return resultado;
    }

    public Map<String, Cultura> listarCulturas() {
        return culturaRepository.listarTodas();
    }

    public MotorRegras getMotorRegras() {
        return motorRegras;
    }

    public String getCidade() { return cidade; }
    public String getPais() { return pais; }
}
