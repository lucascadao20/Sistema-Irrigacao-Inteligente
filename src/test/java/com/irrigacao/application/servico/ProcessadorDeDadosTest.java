package com.irrigacao.application.servico;

import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.modelo.Sensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessadorDeDadosTest {

    private ProcessadorDeDados processador;
    private GerenciadorDeSensores gerenciador;
    private Sensor sensorUmidade;

    @BeforeEach
    void setUp() {
        gerenciador = new GerenciadorDeSensores();
        processador = new ProcessadorDeDados(gerenciador);
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
