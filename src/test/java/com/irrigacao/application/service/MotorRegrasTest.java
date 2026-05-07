package com.irrigacao.application.service;

import com.irrigacao.application.factory.StrategyFactory;
import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Alerta;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotorRegrasTest {

    private MotorRegras motorRegras;
    private List<Alerta> alertasRecebidos;
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @BeforeEach
    void setUp() {
        alertasRecebidos = new ArrayList<>();
        NotificadorAlerta notificador = alertasRecebidos::add;
        motorRegras = new MotorRegras(new StrategyFactory(), notificador);
    }

    @Test
    void deveAtivarIrrigacaoQuandoSoloSeco() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(30.0).umidadeAr(40.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = motorRegras.avaliarEExecutar(25.0, clima, milho);

        assertEquals(StatusIrrigacao.ATIVADA, resultado.getStatus());
        assertTrue(resultado.getVolumeAgua() > 0);
        assertFalse(alertasRecebidos.isEmpty());
    }

    @Test
    void deveRetornarAguardandoQuandoUmidadeAdequada() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(25.0).umidadeAr(50.0).velocidadeVento(2.0)
                .descricaoClima("limpo").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = motorRegras.avaliarEExecutar(55.0, clima, milho);

        assertEquals(StatusIrrigacao.AGUARDANDO, resultado.getStatus());
        assertEquals(0, resultado.getVolumeAgua());
    }

    @Test
    void deveEmitirAlertaDeEmergencia() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(5.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        motorRegras.avaliarEExecutar(10.0, clima, milho);

        assertTrue(alertasRecebidos.stream()
                .anyMatch(a -> a.getNivel() == com.irrigacao.domain.enums.NivelAlerta.EMERGENCIA));
    }

    @Test
    void deveSuspenderComPrevisaoDeChuvaForte() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(22.0).umidadeAr(85.0).velocidadeVento(3.0)
                .descricaoClima("chuva").previsaoChuva(true).volumeChuva(15.0).cidade("SP")
                .build();

        Irrigacao resultado = motorRegras.avaliarEExecutar(40.0, clima, milho);

        assertEquals(StatusIrrigacao.SUSPENSA, resultado.getStatus());
    }
}
