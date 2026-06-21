package com.irrigacao.simulador;

import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeradorLeituraProgressivoTest {

    @Test
    void umidadeSoloDeveFicarSempreEntreLimites() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(42L);
        for (int i = 0; i < 1_000; i++) {
            double v = g.proximaLeitura(TipoSensor.UMIDADE_SOLO);
            assertTrue(v >= 10.0 && v <= 95.0,
                    "umidade fora dos limites no tick " + i + ": " + v);
        }
    }

    @Test
    void temperaturaDeveFicarPertoDaFaixaSenoidal() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(42L);
        for (int i = 0; i < 500; i++) {
            double v = g.proximaLeitura(TipoSensor.TEMPERATURA);
            assertTrue(v >= 17.0 && v <= 31.0,
                    "temperatura fora da faixa esperada no tick " + i + ": " + v);
        }
    }

    @Test
    void umidadeArDeveFicarEntreLimites() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(42L);
        for (int i = 0; i < 500; i++) {
            double v = g.proximaLeitura(TipoSensor.UMIDADE_AR);
            assertTrue(v >= 30.0 && v <= 95.0,
                    "umidade do ar fora dos limites no tick " + i + ": " + v);
        }
    }

    @Test
    void phDeveFicarEntreLimitesFisiologicos() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(42L);
        for (int i = 0; i < 500; i++) {
            double v = g.proximaLeitura(TipoSensor.PH_SOLO);
            assertTrue(v >= 5.5 && v <= 7.5,
                    "pH fora dos limites no tick " + i + ": " + v);
        }
    }

    @Test
    void mesmaSeedDeveGerarSequenciaIdentica() {
        GeradorLeituraProgressivo g1 = new GeradorLeituraProgressivo(123L);
        GeradorLeituraProgressivo g2 = new GeradorLeituraProgressivo(123L);
        for (int i = 0; i < 50; i++) {
            assertEquals(g1.proximaLeitura(TipoSensor.UMIDADE_SOLO),
                         g2.proximaLeitura(TipoSensor.UMIDADE_SOLO),
                         "divergencia no tick " + i);
        }
    }

    @Test
    void leiturasDevemSerArredondadasA1CasaDecimal() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(7L);
        for (int i = 0; i < 50; i++) {
            double v = g.proximaLeitura(TipoSensor.UMIDADE_SOLO);
            double arredondado = Math.round(v * 10.0) / 10.0;
            assertEquals(arredondado, v, 0.0001, "valor sem arredondamento: " + v);
        }
    }
}
