package com.irrigacao.domain.modelo;

import com.irrigacao.domain.enums.StatusIrrigacao;
import java.time.LocalDateTime;

public class Irrigacao {
    private final String id;
    private final Cultura cultura;
    private StatusIrrigacao status;
    private final double volumeAgua;
    private final LocalDateTime inicio;
    private LocalDateTime fim;
    private final String motivo;

    private Irrigacao(Builder builder) {
        this.id = builder.id;
        this.cultura = builder.cultura;
        this.status = builder.status;
        this.volumeAgua = builder.volumeAgua;
        this.motivo = builder.motivo;
        this.inicio = LocalDateTime.now();
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public Cultura getCultura() { return cultura; }
    public StatusIrrigacao getStatus() { return status; }
    public double getVolumeAgua() { return volumeAgua; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public String getMotivo() { return motivo; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public void setStatus(StatusIrrigacao status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Irrigacao[%s] %s | Cultura: %s | Volume: %.1fL | Motivo: %s",
                id, status.getDescricao(), cultura.getNome(), volumeAgua, motivo);
    }

    public static class Builder {
        private String id;
        private Cultura cultura;
        private StatusIrrigacao status;
        private double volumeAgua;
        private String motivo = "";

        public Builder id(String id) { this.id = id; return this; }
        public Builder cultura(Cultura cultura) { this.cultura = cultura; return this; }
        public Builder status(StatusIrrigacao status) { this.status = status; return this; }
        public Builder volumeAgua(double volumeAgua) { this.volumeAgua = volumeAgua; return this; }
        public Builder motivo(String motivo) { this.motivo = motivo; return this; }

        public Irrigacao build() { return new Irrigacao(this); }
    }
}
