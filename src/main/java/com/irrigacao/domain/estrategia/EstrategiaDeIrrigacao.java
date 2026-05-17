package com.irrigacao.domain.estrategia;

import com.irrigacao.domain.modelo.Cultura;
import com.irrigacao.domain.modelo.DadosClimaticos;
import com.irrigacao.domain.modelo.Irrigacao;

public interface EstrategiaDeIrrigacao {
    String getNome();
    Irrigacao calcularIrrigacao(double umidadeSolo, DadosClimaticos clima, Cultura cultura);
}
