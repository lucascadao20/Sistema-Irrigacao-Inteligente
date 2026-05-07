package com.irrigacao.domain.strategy;

import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;

public interface IrrigacaoStrategy {
    String getNome();
    Irrigacao calcularIrrigacao(double umidadeSolo, DadosClimaticos clima, Cultura cultura);
}
