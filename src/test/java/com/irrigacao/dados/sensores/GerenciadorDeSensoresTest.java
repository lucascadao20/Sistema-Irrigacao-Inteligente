package com.irrigacao.dados.sensores;

import com.irrigacao.modelo.TipoSensor;
import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class GerenciadorDeSensoresTest {

    private GerenciadorDeSensores gerenciador;

    @BeforeEach
    void setUp() {
        gerenciador = new GerenciadorDeSensores();
    }

    @Test
    void deveRegistrarSensor() {
        Sensor sensor = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Norte");
        gerenciador.registrarSensor(sensor);
        assertEquals(1, gerenciador.getTotalSensores());
    }

    @Test
    void deveRegistrarLeituraValida() {
        Sensor sensor = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Norte");
        gerenciador.registrarSensor(sensor);

        LeituraSensor leitura = new LeituraSensor(sensor, 45.0, LocalDateTime.now());
        gerenciador.registrarLeitura(leitura);

        Optional<LeituraSensor> ultima = gerenciador.getUltimaLeitura("SU-001");
        assertTrue(ultima.isPresent());
        assertEquals(45.0, ultima.get().getValor());
    }

    @Test
    void deveDescartarLeituraInvalida() {
        Sensor sensor = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Norte");
        gerenciador.registrarSensor(sensor);

        LeituraSensor valida = new LeituraSensor(sensor, 50.0, LocalDateTime.now());
        gerenciador.registrarLeitura(valida);

        LeituraSensor invalida = new LeituraSensor(sensor, -10.0, LocalDateTime.now());
        gerenciador.registrarLeitura(invalida);

        Optional<LeituraSensor> ultima = gerenciador.getUltimaLeitura("SU-001");
        assertTrue(ultima.isPresent());
        assertEquals(50.0, ultima.get().getValor());
    }

    @Test
    void deveRetornarUltimaLeituraPorTipo() {
        Sensor sensor = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Norte");
        gerenciador.registrarSensor(sensor);

        LeituraSensor leitura = new LeituraSensor(sensor, 42.0, LocalDateTime.now());
        gerenciador.registrarLeitura(leitura);

        Optional<Double> valor = gerenciador.getUltimaLeituraPorTipo(TipoSensor.UMIDADE_SOLO);
        assertTrue(valor.isPresent());
        assertEquals(42.0, valor.get());
    }

    @Test
    void deveRetornarSensoresAtivos() {
        Sensor ativo = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Norte");
        Sensor inativo = new Sensor("SU-002", TipoSensor.UMIDADE_SOLO, "Sul");
        inativo.setAtivo(false);

        gerenciador.registrarSensor(ativo);
        gerenciador.registrarSensor(inativo);

        assertEquals(1, gerenciador.getSensoresAtivos().size());
    }
}
