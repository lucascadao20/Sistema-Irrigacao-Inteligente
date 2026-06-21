package com.irrigacao.dados.bd;

public record ConsumoCultura(
        String culturaNome,
        double volumeTotal,
        long qtdIrrigacoes
) {}
