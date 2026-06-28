package com.irrigacao.negocio;

import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;

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
