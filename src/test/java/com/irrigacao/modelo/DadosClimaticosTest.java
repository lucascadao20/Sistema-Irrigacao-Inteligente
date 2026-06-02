package com.irrigacao.modelo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DadosClimaticosTest {

    @Test
    void builderDeveCriarObjetoComTodosOsCampos() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(28.5)
                .umidadeAr(65.0)
                .velocidadeVento(3.2)
                .descricaoClima("ceu limpo")
                .previsaoChuva(false)
                .volumeChuva(0)
                .cidade("Sao Paulo")
                .build();

        assertEquals(28.5, clima.getTemperatura());
        assertEquals(65.0, clima.getUmidadeAr());
        assertEquals(3.2, clima.getVelocidadeVento());
        assertEquals("ceu limpo", clima.getDescricaoClima());
        assertFalse(clima.isPrevisaoChuva());
        assertEquals(0, clima.getVolumeChuva());
        assertEquals("Sao Paulo", clima.getCidade());
    }

    @Test
    void builderDeveUsarValoresPadraoQuandoNaoDefinidos() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(20.0)
                .cidade("Campinas")
                .build();

        assertEquals(20.0, clima.getTemperatura());
        assertEquals(0, clima.getUmidadeAr());
        assertFalse(clima.isPrevisaoChuva());
        assertEquals("Campinas", clima.getCidade());
    }

    @Test
    void toStringDeveConterInformacoesPrincipais() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(30.0)
                .umidadeAr(80.0)
                .velocidadeVento(5.0)
                .descricaoClima("chuva leve")
                .previsaoChuva(true)
                .volumeChuva(12.5)
                .cidade("Ribeirao Preto")
                .build();

        String resultado = clima.toString();
        assertTrue(resultado.contains("Ribeirao Preto"));
        assertTrue(resultado.contains("30,0") || resultado.contains("30.0"));
    }
}
