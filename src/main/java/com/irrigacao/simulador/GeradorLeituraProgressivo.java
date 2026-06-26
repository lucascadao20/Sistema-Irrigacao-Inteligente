package com.irrigacao.simulador;

import com.irrigacao.modelo.TipoSensor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Gerador de leituras com dinamica progressiva para os 4 tipos de sensor.
 *
 * <p>A <strong>umidade do solo</strong> e mantida <em>por cultura</em> — cada
 * cultura tem seu proprio "talhao" simulado, com trajetoria independente
 * (decaimento + eventos esporadicos de chuva). Os demais sensores
 * (temperatura, umidade do ar, pH) sao globais.
 */
public final class GeradorLeituraProgressivo {

    private static final class EstadoSolo {
        double valor = 50.0;
        int ticksParaProximoEvento;
        int ticksDeChuvaRestantes = 0;
        double incrementoPorTickDeChuva = 0;

        EstadoSolo(int intervaloInicial) {
            this.ticksParaProximoEvento = intervaloInicial;
        }
    }

    private final Random random;
    private final Map<String, EstadoSolo> estadoSoloPorCultura = new HashMap<>();

    private int tickTemperatura = 0;
    private double ultimaTemperatura = 24.0;

    private double ph = 6.5;

    public GeradorLeituraProgressivo() {
        this(System.nanoTime());
    }

    public GeradorLeituraProgressivo(long seed) {
        this.random = new Random(seed);
    }

    /** Para sensores globais (temperatura, umidade do ar, pH). */
    public double proximaLeitura(TipoSensor tipo) {
        return switch (tipo) {
            case TEMPERATURA -> arredondar(proximaTemperatura());
            case UMIDADE_AR  -> arredondar(proximaUmidadeAr());
            case PH_SOLO     -> arredondar(proximoPh());
            case UMIDADE_SOLO -> throw new IllegalArgumentException(
                    "UMIDADE_SOLO requer cultura; use proximaLeituraSolo(cultura)");
        };
    }

    /**
     * Geracao para umidade do solo, por cultura.
     *
     * <p>Modelo simplificado para demo: combina <strong>mean reversion</strong>
     * (o solo gravita em torno de uma umidade-meta de 50%) com eventos
     * esporadicos de chuva que empurram a umidade pra cima por varios ticks.
     * Garante oscilacao saudavel sem travar nos limites em execucoes longas.
     */
    public double proximaLeituraSolo(String cultura) {
        EstadoSolo estado = estadoSoloPorCultura.computeIfAbsent(
                cultura, c -> novoEstadoSolo());

        // Mean reversion: puxa em direcao a META proporcionalmente a distancia.
        // Sem isso, decay fixo + rain podia saturar num dos extremos.
        estado.valor -= (estado.valor - META_UMIDADE) * FORCA_ATRACAO;

        if (estado.ticksDeChuvaRestantes > 0) {
            // Chuva em andamento: aplica fracao deste tick e decrementa.
            estado.valor += estado.incrementoPorTickDeChuva;
            estado.ticksDeChuvaRestantes--;
        } else {
            // Sem chuva: decrementa o contador para o proximo evento.
            if (--estado.ticksParaProximoEvento <= 0) {
                int duracao = 6 + random.nextInt(7);                   // 6..12 ticks
                double totalChuva = 5.0 + random.nextDouble() * 8.0;   // +5..+13% total
                estado.ticksDeChuvaRestantes = duracao;
                estado.incrementoPorTickDeChuva = totalChuva / duracao;
                estado.ticksParaProximoEvento = sortearIntervaloEvento();
            }
        }

        double ruido = (random.nextDouble() - 0.5) * 0.3;
        double valor = estado.valor + ruido;
        return arredondar(Math.max(10.0, Math.min(95.0, valor)));
    }

    private static final double META_UMIDADE = 50.0;
    private static final double FORCA_ATRACAO = 0.04; // % da distancia por tick

    private EstadoSolo novoEstadoSolo() {
        // Comeca em valores diferentes por cultura para o primeiro tick ja
        // mostrar diversidade no dashboard.
        EstadoSolo s = new EstadoSolo(sortearIntervaloEvento());
        s.valor = 35.0 + random.nextDouble() * 30.0;
        return s;
    }

    private double proximaTemperatura() {
        double base = 24.0 + 6.0 * Math.sin(2 * Math.PI * tickTemperatura / 60.0);
        double ruido = (random.nextDouble() - 0.5) * 0.6;
        ultimaTemperatura = base + ruido;
        tickTemperatura++;
        return ultimaTemperatura;
    }

    private double proximaUmidadeAr() {
        double base = 65.0 - (ultimaTemperatura - 24.0) * 2.0;
        double ruido = (random.nextDouble() - 0.5) * 2.0;
        return Math.max(30.0, Math.min(95.0, base + ruido));
    }

    private double proximoPh() {
        double passo = (random.nextDouble() - 0.5) * 0.04;
        ph += passo;
        ph = Math.max(5.5, Math.min(7.5, ph));
        return ph;
    }

    private int sortearIntervaloEvento() {
        return 15 + random.nextInt(15); // 15..29 ticks entre chuvas (75s..145s)
    }

    private static double arredondar(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
