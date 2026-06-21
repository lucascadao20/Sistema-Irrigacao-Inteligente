package com.irrigacao.dados.bd;

import com.irrigacao.modelo.StatusIrrigacao;

import java.time.LocalDateTime;

public record RegistroIrrigacao(
        String id,
        String culturaNome,
        StatusIrrigacao status,
        double volumeAgua,
        String motivo,
        String estrategiaNome,
        double umidadeSolo,
        LocalDateTime decididoEm
) {}
