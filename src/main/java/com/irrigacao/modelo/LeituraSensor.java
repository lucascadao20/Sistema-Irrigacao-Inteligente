package com.irrigacao.modelo;

import java.time.LocalDateTime;

public class LeituraSensor {
    private final Sensor sensor;
    private final double valor;
    private final LocalDateTime timestamp;
    private final boolean valida;

    public LeituraSensor(Sensor sensor, double valor, LocalDateTime timestamp) {
        this.sensor = sensor;
        this.valor = valor;
        this.timestamp = timestamp;
        this.valida = validarLeitura(valor);
    }

    private boolean validarLeitura(double valor) {
        return switch (sensor.getTipo()) {
            case UMIDADE_SOLO, UMIDADE_AR -> valor >= 0 && valor <= 100;
            case TEMPERATURA -> valor >= -50 && valor <= 60;
            case PH_SOLO -> valor >= 0 && valor <= 14;
        };
    }

    public Sensor getSensor() { return sensor; }
    public double getValor() { return valor; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isValida() { return valida; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %.1f%s %s",
                timestamp.toLocalTime(),
                sensor.getTipo().getDescricao(),
                valor,
                sensor.getTipo().getUnidade(),
                valida ? "OK" : "INVALIDA");
    }
}
