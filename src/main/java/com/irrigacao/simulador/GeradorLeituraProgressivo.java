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

    /** Para a umidade do solo de uma cultura especifica. */
    public double proximaLeituraSolo(String cultura) {
        EstadoSolo estado = estadoSoloPorCultura.computeIfAbsent(
                cultura, c -> novoEstadoSolo());

        estado.valor -= 0.3;
        if (--estado.ticksParaProximoEvento <= 0) {
            estado.valor += 15.0 + random.nextDouble() * 15.0;
            estado.ticksParaProximoEvento = sortearIntervaloEvento();
        }
        // Ruido menor que o decaimento (0.3/tick) para a tendencia ficar visivel.
        double ruido = (random.nextDouble() - 0.5) * 0.3;
        double valor = estado.valor + ruido;
        return arredondar(Math.max(10.0, Math.min(95.0, valor)));
    }

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
        return 8 + random.nextInt(8); // 8..15
    }

    private static double arredondar(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
