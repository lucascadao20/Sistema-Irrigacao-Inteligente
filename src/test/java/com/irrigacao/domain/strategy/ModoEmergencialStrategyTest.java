package com.irrigacao.domain.strategy;

import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModoEmergencialStrategyTest {

    private final ModoEmergencialStrategy strategy = new ModoEmergencialStrategy();
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @Test
    void deveAtivarIrrigacaoComVolumeAlto() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(5.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = strategy.calcularIrrigacao(10.0, clima, milho);

        assertEquals(StatusIrrigacao.ATIVADA, resultado.getStatus());
        assertTrue(resultado.getVolumeAgua() > 50.0);
    }

    @Test
    void volumeDeveSerMaiorQueModoPadrao() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(30.0).umidadeAr(40.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        ModoSecoStrategy seco = new ModoSecoStrategy();
        Irrigacao resultadoSeco = seco.calcularIrrigacao(15.0, clima, milho);
        Irrigacao resultadoEmergencial = strategy.calcularIrrigacao(15.0, clima, milho);

        assertTrue(resultadoEmergencial.getVolumeAgua() > resultadoSeco.getVolumeAgua());
    }

    @Test
    void getNomeDeveRetornarModoEmergencial() {
        assertEquals("Modo Emergencial", strategy.getNome());
    }
}
