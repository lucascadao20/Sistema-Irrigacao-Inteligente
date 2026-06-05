package com.irrigacao.ui.web;

import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.modelo.NivelAlerta;
import com.irrigacao.modelo.StatusIrrigacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadoDoDashboardTest {

    private Irrigacao irrigacaoFake(double volume) {
        return Irrigacao.builder()
                .id("X1")
                .cultura(new Cultura("Milho", 30, 60, 650, 1.15))
                .status(StatusIrrigacao.ATIVADA)
                .volumeAgua(volume)
                .motivo("teste")
                .build();
    }

    private DadosClimaticos climaFake() {
        return DadosClimaticos.builder().cidade("Sao Paulo").temperatura(25).build();
    }

    @Test
    void deveTerValoresPadraoAntesDoPrimeiroCiclo() {
        EstadoDoDashboard estado = new EstadoDoDashboard();
        assertEquals("milho", estado.getCulturaAtiva());
        assertEquals("-", estado.getEstrategiaAtual());
        assertNull(estado.getUltimoClima());
        assertNull(estado.getUltimaIrrigacao());
        assertTrue(estado.getHistorico().isEmpty());
        assertTrue(estado.getAlertas().isEmpty());
    }

    @Test
    void registrarCicloAtualizaEstadoEHistorico() {
        EstadoDoDashboard estado = new EstadoDoDashboard();

        estado.registrarCiclo(42.5, climaFake(), irrigacaoFake(100), "Modo Seco");

        assertEquals(42.5, estado.getUltimaUmidadeSolo());
        assertEquals("Modo Seco", estado.getEstrategiaAtual());
        assertEquals("Sao Paulo", estado.getUltimoClima().getCidade());
        assertEquals(100, estado.getUltimaIrrigacao().getVolumeAgua());
        assertEquals(1, estado.getHistorico().size());
        assertEquals(42.5, estado.getHistorico().get(0).umidadeSolo());
    }

    @Test
    void historicoNaoUltrapassaLimiteMaximo() {
        EstadoDoDashboard estado = new EstadoDoDashboard();
        for (int i = 0; i < 80; i++) {
            estado.registrarCiclo(i, climaFake(), irrigacaoFake(i), "Modo Seco");
        }
        assertEquals(60, estado.getHistorico().size());
        // O mais antigo retido deve ser o ciclo 20 (os 20 primeiros foram descartados).
        assertEquals(20.0, estado.getHistorico().get(0).umidadeSolo());
    }

    @Test
    void alertasNaoUltrapassamLimiteEVoltamMaisRecentePrimeiro() {
        EstadoDoDashboard estado = new EstadoDoDashboard();
        for (int i = 0; i < 40; i++) {
            estado.registrarAlerta(new Alerta("A" + i, NivelAlerta.INFO, "msg" + i));
        }
        List<Alerta> alertas = estado.getAlertas();
        assertEquals(30, alertas.size());
        // getAlertas retorna o mais recente primeiro.
        assertEquals("msg39", alertas.get(0).getMensagem());
        assertEquals("msg10", alertas.get(29).getMensagem());
    }
}
