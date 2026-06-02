package com.irrigacao.domain.estrategia;

import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EstrategiaModoEmergencialTest {

    private final EstrategiaModoEmergencial estrategia = new EstrategiaModoEmergencial();
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @Test
    void deveAtivarIrrigacaoComVolumeAlto() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(5.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = estrategia.calcularIrrigacao(10.0, clima, milho);

        assertEquals(StatusIrrigacao.ATIVADA, resultado.getStatus());
        assertTrue(resultado.getVolumeAgua() > 50.0);
    }

    @Test
    void volumeDeveSerMaiorQueModoPadrao() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(30.0).umidadeAr(40.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        EstrategiaModoSeco seco = new EstrategiaModoSeco();
        Irrigacao resultadoSeco = seco.calcularIrrigacao(15.0, clima, milho);
        Irrigacao resultadoEmergencial = estrategia.calcularIrrigacao(15.0, clima, milho);

        assertTrue(resultadoEmergencial.getVolumeAgua() > resultadoSeco.getVolumeAgua());
    }

    @Test
    void getNomeDeveRetornarModoEmergencial() {
        assertEquals("Modo Emergencial", estrategia.getNome());
    }
}
