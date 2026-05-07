package com.irrigacao.domain.model;

public class Cultura {
    private final String nome;
    private final double umidadeMinima;
    private final double umidadeIdeal;
    private final double necessidadeHidrica;
    private final double coeficienteCultura;

    public Cultura(String nome, double umidadeMinima, double umidadeIdeal,
                   double necessidadeHidrica, double coeficienteCultura) {
        this.nome = nome;
        this.umidadeMinima = umidadeMinima;
        this.umidadeIdeal = umidadeIdeal;
        this.necessidadeHidrica = necessidadeHidrica;
        this.coeficienteCultura = coeficienteCultura;
    }

    public String getNome() { return nome; }
    public double getUmidadeMinima() { return umidadeMinima; }
    public double getUmidadeIdeal() { return umidadeIdeal; }
    public double getNecessidadeHidrica() { return necessidadeHidrica; }
    public double getCoeficienteCultura() { return coeficienteCultura; }

    @Override
    public String toString() {
        return String.format("Cultura[%s] Umidade Min: %.0f%% | Ideal: %.0f%% | Necessidade: %.0f mm | Kc: %.2f",
                nome, umidadeMinima, umidadeIdeal, necessidadeHidrica, coeficienteCultura);
    }
}
