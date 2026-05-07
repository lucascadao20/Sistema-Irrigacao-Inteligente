package com.irrigacao.application.factory;

import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.strategy.IrrigacaoStrategy;
import com.irrigacao.domain.strategy.ModoEmergencialStrategy;
import com.irrigacao.domain.strategy.ModoSecoStrategy;
import com.irrigacao.domain.strategy.ModoUmidoStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StrategyFactoryTest {

    private final StrategyFactory factory = new StrategyFactory();
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    private DadosClimaticos climaSeco() {
        return DadosClimaticos.builder()
                .temperatura(30.0).umidadeAr(40.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();
    }

    private DadosClimaticos climaChuvoso() {
        return DadosClimaticos.builder()
                .temperatura(22.0).umidadeAr(85.0).velocidadeVento(3.0)
                .descricaoClima("chuva").previsaoChuva(true).volumeChuva(10.0).cidade("SP")
                .build();
    }

    @Test
    void deveRetornarEmergencialQuandoUmidadeMuitoBaixa() {
        IrrigacaoStrategy strategy = factory.criar(15.0, climaSeco(), milho);
        assertInstanceOf(ModoEmergencialStrategy.class, strategy);
    }

    @Test
    void deveRetornarUmidoQuandoPrevisaoChuva() {
        IrrigacaoStrategy strategy = factory.criar(25.0, climaChuvoso(), milho);
        assertInstanceOf(ModoUmidoStrategy.class, strategy);
    }

    @Test
    void deveRetornarUmidoQuandoUmidadeArAlta() {
        DadosClimaticos climaUmido = DadosClimaticos.builder()
                .temperatura(22.0).umidadeAr(85.0).velocidadeVento(2.0)
                .descricaoClima("nublado").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();
        IrrigacaoStrategy strategy = factory.criar(25.0, climaUmido, milho);
        assertInstanceOf(ModoUmidoStrategy.class, strategy);
    }

    @Test
    void deveRetornarSecoQuandoAbaixoDoMinimo() {
        IrrigacaoStrategy strategy = factory.criar(25.0, climaSeco(), milho);
        assertInstanceOf(ModoSecoStrategy.class, strategy);
    }

    @Test
    void deveRetornarNullQuandoUmidadeAdequada() {
        IrrigacaoStrategy strategy = factory.criar(55.0, climaSeco(), milho);
        assertNull(strategy);
    }
}
