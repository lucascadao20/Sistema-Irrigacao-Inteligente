package com.irrigacao.negocio;

import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.negocio.EstrategiaDeIrrigacao;
import com.irrigacao.negocio.EstrategiaModoEmergencial;
import com.irrigacao.negocio.EstrategiaModoSeco;
import com.irrigacao.negocio.EstrategiaModoUmido;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FabricaDeEstrategiaTest {

    private final FabricaDeEstrategia fabrica = new FabricaDeEstrategia();
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
        EstrategiaDeIrrigacao estrategia = fabrica.criar(15.0, climaSeco(), milho);
        assertInstanceOf(EstrategiaModoEmergencial.class, estrategia);
    }

    @Test
    void deveRetornarUmidoQuandoPrevisaoChuva() {
        EstrategiaDeIrrigacao estrategia = fabrica.criar(40.0, climaChuvoso(), milho);
        assertInstanceOf(EstrategiaModoUmido.class, estrategia);
    }

    @Test
    void deveRetornarUmidoQuandoUmidadeArAlta() {
        DadosClimaticos climaUmido = DadosClimaticos.builder()
                .temperatura(22.0).umidadeAr(85.0).velocidadeVento(2.0)
                .descricaoClima("nublado").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();
        EstrategiaDeIrrigacao estrategia = fabrica.criar(40.0, climaUmido, milho);
        assertInstanceOf(EstrategiaModoUmido.class, estrategia);
    }

    @Test
    void deveRetornarSecoQuandoAbaixoDoMinimo() {
        EstrategiaDeIrrigacao estrategia = fabrica.criar(25.0, climaSeco(), milho);
        assertInstanceOf(EstrategiaModoSeco.class, estrategia);
    }

    @Test
    void devePriorizarSecoSobreUmidoQuandoSoloAbaixoDoMinimo() {
        EstrategiaDeIrrigacao estrategia = fabrica.criar(25.0, climaChuvoso(), milho);
        assertInstanceOf(EstrategiaModoSeco.class, estrategia);
    }

    @Test
    void deveRetornarNullQuandoUmidadeAdequada() {
        EstrategiaDeIrrigacao estrategia = fabrica.criar(55.0, climaSeco(), milho);
        assertNull(estrategia);
    }
}
