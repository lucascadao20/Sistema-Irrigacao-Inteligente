package com.irrigacao.domain.estrategia;

import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EstrategiaModoSecoTest {

    private final EstrategiaModoSeco estrategia = new EstrategiaModoSeco();
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @Test
    void deveAtivarIrrigacaoEmClimaQuente() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = estrategia.calcularIrrigacao(25.0, clima, milho);

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

        Irrigacao resultadoNormal = estrategia.calcularIrrigacao(25.0, climaNormal, milho);
        Irrigacao resultadoQuente = estrategia.calcularIrrigacao(25.0, climaQuente, milho);

        assertTrue(resultadoQuente.getVolumeAgua() > resultadoNormal.getVolumeAgua());
    }

    @Test
    void getNomeDeveRetornarModoSeco() {
        assertEquals("Modo Seco", estrategia.getNome());
    }
}
