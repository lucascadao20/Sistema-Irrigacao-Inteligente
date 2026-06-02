package com.irrigacao.domain.estrategia;

import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;

public class EstrategiaModoUmido implements EstrategiaDeIrrigacao {

    @Override
    public String getNome() {
        return "Modo Umido";
    }

    @Override
    public Irrigacao calcularIrrigacao(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        if (clima.isPrevisaoChuva() && clima.getVolumeChuva() > 5.0) {
            String motivo = String.format("Previsao de chuva: %.1f mm - Irrigacao suspensa para evitar desperdicio",
                    clima.getVolumeChuva());
            return Irrigacao.builder()
                    .cultura(cultura)
                    .status(StatusIrrigacao.SUSPENSA)
                    .volumeAgua(0)
                    .motivo(motivo)
                    .build();
        }

        if (clima.isPrevisaoChuva()) {
            double volumeReduzido = (cultura.getUmidadeIdeal() - umidadeSolo) * cultura.getCoeficienteCultura() * 3;
            String motivo = String.format("Chuva leve prevista (%.1f mm) - Irrigacao reduzida", clima.getVolumeChuva());
            return Irrigacao.builder()
                    .cultura(cultura)
                    .status(StatusIrrigacao.ATIVADA)
                    .volumeAgua(Math.max(volumeReduzido, 2.0))
                    .motivo(motivo)
                    .build();
        }

        return Irrigacao.builder()
                .cultura(cultura)
                .status(StatusIrrigacao.AGUARDANDO)
                .volumeAgua(0)
                .motivo("Umidade do ar alta, sem necessidade imediata de irrigacao")
                .build();
    }
}
