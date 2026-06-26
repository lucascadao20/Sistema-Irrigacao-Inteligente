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
    void variacaoEntreTicksConsecutivosNaoEhBrusca() {
        // Eventos de chuva sao espalhados em 6-12 ticks. Variacao por tick
        // tem que ficar pequena (incremento medio ~+1.5/tick + ruido ±0.15).
        // Limite generoso de 4% por tick para nao falhar por flutuacao
        // estatistica em sequencias muito longas.
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(42L);
        double anterior = g.proximaLeituraSolo("milho");
        for (int i = 0; i < 2_000; i++) {
            double atual = g.proximaLeituraSolo("milho");
            double delta = Math.abs(atual - anterior);
            assertTrue(delta <= 4.0,
                    "salto brusco no tick " + i + ": " + anterior + " -> " + atual
                            + " (delta=" + delta + ")");
            anterior = atual;
        }
    }

    @Test
    void umidadeNaoFicaTravadaNosLimitesEmExecucaoLonga() {
        // Roda 5_000 ticks (= ~7h reais a 5s/tick) e mede a fracao do tempo
        // que o valor passou nos extremos. Se sistema for desbalanceado,
        // satura num dos lados.
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(99L);
        int travadoNoTopo = 0, travadoNoFundo = 0;
        double soma = 0;
        int n = 5_000;
        for (int i = 0; i < n; i++) {
            double v = g.proximaLeituraSolo("milho");
            soma += v;
            if (v >= 94.0) travadoNoTopo++;
            if (v <= 11.0) travadoNoFundo++;
        }
        double media = soma / n;
        // Sistema saudavel: deve passar < 5% do tempo em cada extremo.
        assertTrue(travadoNoTopo < n * 0.05,
                "travou no topo " + travadoNoTopo + "/" + n + " ticks (media=" + media + ")");
        assertTrue(travadoNoFundo < n * 0.05,
                "travou no fundo " + travadoNoFundo + "/" + n + " ticks (media=" + media + ")");
        // Media de longo prazo deve ficar numa faixa razoavel (nao colada nos limites).
        assertTrue(media >= 25 && media <= 70,
                "media fora da faixa saudavel: " + media);
    }

    @Test
    void chamarProximaLeituraDeSoloLancaErro() {
        GeradorLeituraProgressivo g = new GeradorLeituraProgressivo(1L);
        assertThrows(IllegalArgumentException.class,
                () -> g.proximaLeitura(TipoSensor.UMIDADE_SOLO));
    }
}
