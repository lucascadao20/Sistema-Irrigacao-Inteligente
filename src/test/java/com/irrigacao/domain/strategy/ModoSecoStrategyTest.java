package com.irrigacao.domain.strategy;

import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModoSecoStrategyTest {

    private final ModoSecoStrategy strategy = new ModoSecoStrategy();
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @Test
    void deveAtivarIrrigacaoEmClimaQuente() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = strategy.calcularIrrigacao(25.0, clima, milho);

        assertEquals(StatusIrrigacao.ATIVADA, resultado.getStatus());
        assertTrue(resultado.getVolumeAgua() >= 5.0);
    }

    @Test
    void deveAumentarVolumeComTemperaturaAlta() {
        DadosClimaticos climaNormal = DadosClimaticos.builder()
                .temperatura(25.0).umidadeAr(30.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();
        DadosClimaticos climaQuente = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultadoNormal = strategy.calcularIrrigacao(25.0, climaNormal, milho);
        Irrigacao resultadoQuente = strategy.calcularIrrigacao(25.0, climaQuente, milho);

        assertTrue(resultadoQuente.getVolumeAgua() > resultadoNormal.getVolumeAgua());
    }

    @Test
    void getNomeDeveRetornarModoSeco() {
        assertEquals("Modo Seco", strategy.getNome());
    }
}
