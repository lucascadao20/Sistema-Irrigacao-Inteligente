package com.irrigacao.domain.strategy;

import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;

public class ModoEmergencialStrategy implements IrrigacaoStrategy {

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
