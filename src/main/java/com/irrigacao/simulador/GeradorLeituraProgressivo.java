package com.irrigacao.simulador;

import com.irrigacao.modelo.TipoSensor;

import java.util.Random;

public final class GeradorLeituraProgressivo {

    private final Random random;

    private double umidadeSolo = 50.0;
    private int ticksParaProximoEvento;

    private int tickTemperatura = 0;
    private double ultimaTemperatura = 24.0;

    private double ph = 6.5;

    public GeradorLeituraProgressivo() {
        this(System.nanoTime());
    }

    public GeradorLeituraProgressivo(long seed) {
        this.random = new Random(seed);
        this.ticksParaProximoEvento = sortearIntervaloEvento();
    }

    public double proximaLeitura(TipoSensor tipo) {
        return switch (tipo) {
            case UMIDADE_SOLO -> arredondar(proximaUmidadeSolo());
            case TEMPERATURA  -> arredondar(proximaTemperatura());
            case UMIDADE_AR   -> arredondar(proximaUmidadeAr());
            case PH_SOLO      -> arredondar(proximoPh());
        };
    }

    private double proximaUmidadeSolo() {
        umidadeSolo -= 0.3;
        if (--ticksParaProximoEvento <= 0) {
            umidadeSolo += 15.0 + random.nextDouble() * 15.0;
            ticksParaProximoEvento = sortearIntervaloEvento();
        }
        // Ruido menor que o decaimento (0.3/tick) para a tendencia ficar visivel.
        double ruido = (random.nextDouble() - 0.5) * 0.3;
        double valor = umidadeSolo + ruido;
        return Math.max(10.0, Math.min(95.0, valor));
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
