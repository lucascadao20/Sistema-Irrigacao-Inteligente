package com.irrigacao.negocio;

import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.negocio.EstrategiaDeIrrigacao;
import com.irrigacao.negocio.EstrategiaModoEmergencial;
import com.irrigacao.negocio.EstrategiaModoSeco;
import com.irrigacao.negocio.EstrategiaModoUmido;

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
