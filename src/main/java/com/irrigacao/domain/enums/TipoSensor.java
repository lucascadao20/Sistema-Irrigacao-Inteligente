package com.irrigacao.domain.enums;

public enum TipoSensor {
    UMIDADE_SOLO("Umidade do Solo", "%"),
    TEMPERATURA("Temperatura", "°C"),
    UMIDADE_AR("Umidade do Ar", "%"),
    PH_SOLO("pH do Solo", "pH");

    private final String descricao;
    private final String unidade;

    TipoSensor(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String getDescricao() { return descricao; }
    public String getUnidade() { return unidade; }
}
