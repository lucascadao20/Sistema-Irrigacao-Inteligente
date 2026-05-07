package com.irrigacao.domain.strategy;

import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;

public class ModoUmidoStrategy implements IrrigacaoStrategy {

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
