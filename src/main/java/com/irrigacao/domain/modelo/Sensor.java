package com.irrigacao.domain.modelo;

import com.irrigacao.domain.enums.TipoSensor;
import java.time.LocalDateTime;

public class Sensor {
    private final String id;
    private final TipoSensor tipo;
    private final String localizacao;
    private boolean ativo;
    private LocalDateTime ultimaLeitura;

    public Sensor(String id, TipoSensor tipo, String localizacao) {
        this.id = id;
        this.tipo = tipo;
        this.localizacao = localizacao;
        this.ativo = true;
    }

    public String getId() { return id; }
    public TipoSensor getTipo() { return tipo; }
    public String getLocalizacao() { return localizacao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getUltimaLeitura() { return ultimaLeitura; }
    public void setUltimaLeitura(LocalDateTime ultimaLeitura) { this.ultimaLeitura = ultimaLeitura; }

    @Override
    public String toString() {
        return String.format("Sensor[%s] %s - %s (%s)", id, tipo.getDescricao(), localizacao, ativo ? "Ativo" : "Inativo");
    }
}
