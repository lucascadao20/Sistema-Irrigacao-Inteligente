package com.irrigacao.negocio;

import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;

public class EstrategiaModoEmergencial implements EstrategiaDeIrrigacao {

    @Override
    public String getNome() {
        return "Modo Emergencial";
    }

    @Override
    public Irrigacao calcularIrrigacao(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        double deficit = cultura.getUmidadeIdeal() - umidadeSolo;
        double volumeEmergencia = deficit * cultura.getCoeficienteCultura() * 15;

        String motivo = String.format("EMERGENCIA: Solo em %.0f%% (critico < %.0f%%) - Irrigacao imediata!",
                umidadeSolo, cultura.getUmidadeMinima());

        return Irrigacao.builder()
                .cultura(cultura)
                .status(StatusIrrigacao.ATIVADA)
                .volumeAgua(volumeEmergencia)
                .motivo(motivo)
                .build();
    }
}
