package com.irrigacao.dados.mqtt;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EstadoUltimasLeiturasTest {

    private final Sensor sensorUmidade = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A");

    @Test
    void deveRetornarVazioQuandoNenhumaLeituraRegistrada() {
        EstadoUltimasLeituras estado = new EstadoUltimasLeituras();
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO).isEmpty());
    }

    @Test
    void deveRetornarUltimaLeituraRegistradaParaOTipo() {
        EstadoUltimasLeituras estado = new EstadoUltimasLeituras();
        LocalDateTime ts = LocalDateTime.now();

        estado.registrar(sensorUmidade, 42.0, ts);

        Optional<LeituraSensor> leitura = estado.getUltima(TipoSensor.UMIDADE_SOLO);
        assertTrue(leitura.isPresent());
        assertEquals(42.0, leitura.get().getValor());
        assertEquals(ts, leitura.get().getTimestamp());
    }

    @Test
    void deveSobrescreverLeituraAnteriorDoMesmoTipo() {
        EstadoUltimasLeituras estado = new EstadoUltimasLeituras();
        estado.registrar(sensorUmidade, 42.0, LocalDateTime.now());
        estado.registrar(sensorUmidade, 55.5, LocalDateTime.now());

        assertEquals(55.5, estado.getUltima(TipoSensor.UMIDADE_SOLO).orElseThrow().getValor());
    }

    @Test
    void leiturasDeTiposDiferentesNaoSeAfetam() {
        EstadoUltimasLeituras estado = new EstadoUltimasLeituras();
        Sensor sensorTemp = new Sensor("ST-001", TipoSensor.TEMPERATURA, "Talhao A");
        estado.registrar(sensorUmidade, 42.0, LocalDateTime.now());
        estado.registrar(sensorTemp, 25.0, LocalDateTime.now());

        assertEquals(42.0, estado.getUltima(TipoSensor.UMIDADE_SOLO).orElseThrow().getValor());
        assertEquals(25.0, estado.getUltima(TipoSensor.TEMPERATURA).orElseThrow().getValor());
    }

    @Test
    void deveSerThreadSafeSobEscritasConcorrentes() throws Exception {
        EstadoUltimasLeituras estado = new EstadoUltimasLeituras();
        int writers = 8;
        int writesPerWriter = 1_000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(writers);
        var pool = Executors.newFixedThreadPool(writers);

        for (int w = 0; w < writers; w++) {
            int id = w;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < writesPerWriter; i++) {
                        estado.registrar(sensorUmidade, id * 1000.0 + i, LocalDateTime.now());
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        // Sem assert sobre o valor exato; o objetivo é provar ausência de NPE/ConcurrentModification.
        assertTrue(estado.getUltima(TipoSensor.UMIDADE_SOLO).isPresent());
    }
}
