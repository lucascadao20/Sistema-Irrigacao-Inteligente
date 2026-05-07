package com.irrigacao.application.factory;

import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.strategy.IrrigacaoStrategy;
import com.irrigacao.domain.strategy.ModoEmergencialStrategy;
import com.irrigacao.domain.strategy.ModoSecoStrategy;
import com.irrigacao.domain.strategy.ModoUmidoStrategy;

public class StrategyFactory {

    public IrrigacaoStrategy criar(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        if (umidadeSolo < cultura.getUmidadeMinima() * 0.7) {
            return new ModoEmergencialStrategy();
        }
        if (clima.isPrevisaoChuva() || clima.getUmidadeAr() > 80) {
            return new ModoUmidoStrategy();
        }
        if (umidadeSolo < cultura.getUmidadeMinima()) {
            return new ModoSecoStrategy();
        }
        return null;
    }
}
