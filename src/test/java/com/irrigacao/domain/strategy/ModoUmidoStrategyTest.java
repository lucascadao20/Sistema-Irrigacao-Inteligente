package com.irrigacao.domain.strategy;

import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModoUmidoStrategyTest {

    private final ModoUmidoStrategy strategy = new ModoUmidoStrategy();
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @Test
    void deveSuspenderIrrigacaoComChuvaPrevista() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(25.0).umidadeAr(85.0).velocidadeVento(2.0)
                .descricaoClima("chuva").previsaoChuva(true).volumeChuva(10.0).cidade("SP")
                .build();

        Irrigacao resultado = strategy.calcularIrrigacao(40.0, clima, milho);

        assertEquals(StatusIrrigacao.SUSPENSA, resultado.getStatus());
        assertEquals(0, resultado.getVolumeAgua());
    }

    @Test
    void deveReduzirIrrigacaoComChuvaLeve() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(25.0).umidadeAr(85.0).velocidadeVento(2.0)
                .descricaoClima("garoa").previsaoChuva(true).volumeChuva(3.0).cidade("SP")
                .build();

        Irrigacao resultado = strategy.calcularIrrigacao(40.0, clima, milho);

        assertEquals(StatusIrrigacao.ATIVADA, resultado.getStatus());
        assertTrue(resultado.getVolumeAgua() > 0);
    }

    @Test
    void deveAguardarComUmidadeAltaSemChuva() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(25.0).umidadeAr(85.0).velocidadeVento(2.0)
                .descricaoClima("nublado").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = strategy.calcularIrrigacao(40.0, clima, milho);

        assertEquals(StatusIrrigacao.AGUARDANDO, resultado.getStatus());
    }
}
