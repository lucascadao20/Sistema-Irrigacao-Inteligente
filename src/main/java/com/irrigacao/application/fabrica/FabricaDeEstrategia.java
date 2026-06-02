package com.irrigacao.application.fabrica;

import com.irrigacao.domain.modelo.Cultura;
import com.irrigacao.domain.modelo.DadosClimaticos;
import com.irrigacao.domain.estrategia.EstrategiaDeIrrigacao;
import com.irrigacao.domain.estrategia.EstrategiaModoEmergencial;
import com.irrigacao.domain.estrategia.EstrategiaModoSeco;
import com.irrigacao.domain.estrategia.EstrategiaModoUmido;

public class FabricaDeEstrategia {

    public EstrategiaDeIrrigacao criar(double umidadeSolo, DadosClimaticos clima, Cultura cultura) {
        if (umidadeSolo < cultura.getUmidadeMinima() * 0.7) {
            return new EstrategiaModoEmergencial();
        }
        if (umidadeSolo < cultura.getUmidadeMinima()) {
            return new EstrategiaModoSeco();
        }
        if (clima.isPrevisaoChuva() || clima.getUmidadeAr() > 80) {
            return new EstrategiaModoUmido();
        }
        return null;
    }
}
