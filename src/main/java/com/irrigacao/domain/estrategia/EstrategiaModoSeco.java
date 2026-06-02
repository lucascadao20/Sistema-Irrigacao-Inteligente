package com.irrigacao.domain.estrategia;

import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;

public class EstrategiaModoSeco implements EstrategiaDeIrrigacao {

    @Override
    public String getNome() {
        return "Modo Seco";
    }

    @Override
    public Irrigacao calcularIrrigacao(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        double deficit = cultura.getUmidadeIdeal() - umidadeSolo;
        double volumeBase = deficit * cultura.getCoeficienteCultura() * 10;

        if (clima.getTemperatura() > 30) {
            volumeBase *= 1.3;
        }

        String motivo = String.format("Clima seco (%.1f°C, umidade ar %.0f%%) - Solo em %.0f%% (minimo: %.0f%%)",
                clima.getTemperatura(), clima.getUmidadeAr(), umidadeSolo, cultura.getUmidadeMinima());

        return Irrigacao.builder()
                .cultura(cultura)
                .status(StatusIrrigacao.ATIVADA)
                .volumeAgua(Math.max(volumeBase, 5.0))
                .motivo(motivo)
                .build();
    }
}
