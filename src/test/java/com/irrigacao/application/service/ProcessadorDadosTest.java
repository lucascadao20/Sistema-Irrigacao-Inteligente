package com.irrigacao.application.service;

import com.irrigacao.domain.enums.TipoSensor;
import com.irrigacao.domain.model.Sensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessadorDadosTest {

    private ProcessadorDados processador;
    private GerenciadorSensores gerenciador;
    private Sensor sensorUmidade;

    @BeforeEach
    void setUp() {
        gerenciador = new GerenciadorSensores();
        processador = new ProcessadorDados(gerenciador);
        sensorUmidade = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Norte");
        gerenciador.registrarSensor(sensorUmidade);
    }

    @Test
    void deveProcessarLeituraValida() {
        processador.processarLeitura(sensorUmidade, 45.0);
        assertEquals(45.0, processador.getUmidadeSolo());
    }

    @Test
    void deveManterUltimaLeituraValidaAposInvalida() {
        processador.processarLeitura(sensorUmidade, 45.0);
        processador.processarLeitura(sensorUmidade, -20.0);
        assertEquals(45.0, processador.getUmidadeSolo());
    }

    @Test
    void deveRetornarPadraoQuandoSemLeitura() {
        assertEquals(50.0, processador.getUmidadeSolo());
    }
}
