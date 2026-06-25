package com.irrigacao.simulador;

import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeradorLeituraProgressivoTest {

    @Test
    void umidadeSoloDeveFicarSempreEntreLimites() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(42L);
        for (int i = 0; i < 1_000; i++) {
            double v = g.proximaLeituraSolo("milho");
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
            assertEquals(g1.proximaLeituraSolo("milho"),
                         g2.proximaLeituraSolo("milho"),
                         "divergencia no tick " + i);
        }
    }

    @Test
    void leiturasDevemSerArredondadasA1CasaDecimal() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(7L);
        for (int i = 0; i < 50; i++) {
            double v = g.proximaLeituraSolo("milho");
            double arredondado = Math.round(v * 10.0) / 10.0;
            assertEquals(arredondado, v, 0.0001, "valor sem arredondamento: " + v);
        }
    }

    @Test
    void culturasDiferentesTemTrajetoriasIndependentes() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(99L);
        // Rodar varios ticks pra cada cultura
        double milhoFinal = 0, sojaFinal = 0;
        for (int i = 0; i < 30; i++) {
            milhoFinal = g.proximaLeituraSolo("milho");
            sojaFinal = g.proximaLeituraSolo("soja");
        }
        // As trajetorias divergem — valores nao podem ser iguais por construcao
        // (estados separados, eventos de chuva em ticks diferentes, ruido distinto).
        assertNotEquals(milhoFinal, sojaFinal,
                "culturas deveriam divergir; milho=" + milhoFinal + " soja=" + sojaFinal);
    }

    @Test
    void chamarProximaLeituraDeSoloLancaErro() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(1L);
        assertThrows(IllegalArgumentException.class,
                () -> g.proximaLeitura(TipoSensor.UMIDADE_SOLO));
    }
}
