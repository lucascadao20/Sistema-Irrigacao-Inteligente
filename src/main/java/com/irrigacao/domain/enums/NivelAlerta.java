package com.irrigacao.domain.enums;

public enum NivelAlerta {
    INFO("Informativo"),
    AVISO("Aviso"),
    CRITICO("Critico"),
    EMERGENCIA("Emergencia");

    private final String descricao;

    NivelAlerta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
}
