package com.irrigacao.domain.estrategia;

import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;

public interface EstrategiaDeIrrigacao {
    String getNome();
    Irrigacao calcularIrrigacao(double umidadeSolo, DadosClimaticos clima, Cultura cultura);
}
