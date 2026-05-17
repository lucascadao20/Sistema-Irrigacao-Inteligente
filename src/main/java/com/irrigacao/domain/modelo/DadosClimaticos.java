package com.irrigacao.domain.modelo;

public class DadosClimaticos {
    private final double temperatura;
    private final double umidadeAr;
    private final double velocidadeVento;
    private final String descricaoClima;
    private final boolean previsaoChuva;
    private final double volumeChuva;
    private final String cidade;

    private DadosClimaticos(Builder builder) {
        this.temperatura = builder.temperatura;
        this.umidadeAr = builder.umidadeAr;
        this.velocidadeVento = builder.velocidadeVento;
        this.descricaoClima = builder.descricaoClima;
        this.previsaoChuva = builder.previsaoChuva;
        this.volumeChuva = builder.volumeChuva;
        this.cidade = builder.cidade;
    }

    public static Builder builder() { return new Builder(); }

    public double getTemperatura() { return temperatura; }
    public double getUmidadeAr() { return umidadeAr; }
    public double getVelocidadeVento() { return velocidadeVento; }
    public String getDescricaoClima() { return descricaoClima; }
    public boolean isPrevisaoChuva() { return previsaoChuva; }
    public double getVolumeChuva() { return volumeChuva; }
    public String getCidade() { return cidade; }

    @Override
    public String toString() {
        return String.format("Clima[%s] Temp: %.1f°C | Umidade: %.0f%% | Vento: %.1f m/s | %s | Chuva: %s (%.1f mm)",
                cidade, temperatura, umidadeAr, velocidadeVento, descricaoClima,
                previsaoChuva ? "Sim" : "Nao", volumeChuva);
    }

    public static class Builder {
        private double temperatura;
        private double umidadeAr;
        private double velocidadeVento;
        private String descricaoClima = "";
        private boolean previsaoChuva;
        private double volumeChuva;
        private String cidade = "";

        public Builder temperatura(double temperatura) { this.temperatura = temperatura; return this; }
        public Builder umidadeAr(double umidadeAr) { this.umidadeAr = umidadeAr; return this; }
        public Builder velocidadeVento(double velocidadeVento) { this.velocidadeVento = velocidadeVento; return this; }
        public Builder descricaoClima(String descricaoClima) { this.descricaoClima = descricaoClima; return this; }
        public Builder previsaoChuva(boolean previsaoChuva) { this.previsaoChuva = previsaoChuva; return this; }
        public Builder volumeChuva(double volumeChuva) { this.volumeChuva = volumeChuva; return this; }
        public Builder cidade(String cidade) { this.cidade = cidade; return this; }

        public DadosClimaticos build() { return new DadosClimaticos(this); }
    }
}
