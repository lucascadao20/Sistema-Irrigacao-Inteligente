package com.irrigacao.modelo;

public enum StatusIrrigacao {
    ATIVADA("Irrigacao Ativada"),
    SUSPENSA("Irrigacao Suspensa"),
    AGUARDANDO("Aguardando Proximo Ciclo");

    private final String descricao;

    StatusIrrigacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
}
