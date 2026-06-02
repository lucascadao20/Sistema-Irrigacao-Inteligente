package com.irrigacao.application.servico;

import com.irrigacao.application.fabrica.FabricaDeEstrategia;
import com.irrigacao.application.interfaces.NotificadorDeAlerta;
import com.irrigacao.modelo.NivelAlerta;
import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotorDeRegrasTest {

    private MotorDeRegras motorDeRegras;
    private List<Alerta> alertasRecebidos;
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @BeforeEach
    void setUp() {
        alertasRecebidos = new ArrayList<>();
        NotificadorDeAlerta notificador = alertasRecebidos::add;
        motorDeRegras = new MotorDeRegras(new FabricaDeEstrategia(), notificador);
    }

    @Test
    void deveAtivarIrrigacaoQuandoSoloSeco() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(30.0).umidadeAr(40.0).velocidadeVento(2.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        Irrigacao resultado = motorDeRegras.avaliarEExecutar(25.0, clima, milho);

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

        Irrigacao resultado = motorDeRegras.avaliarEExecutar(55.0, clima, milho);

        assertEquals(StatusIrrigacao.AGUARDANDO, resultado.getStatus());
        assertEquals(0, resultado.getVolumeAgua());
    }

    @Test
    void deveEmitirAlertaDeEmergencia() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(35.0).umidadeAr(30.0).velocidadeVento(5.0)
                .descricaoClima("seco").previsaoChuva(false).volumeChuva(0).cidade("SP")
                .build();

        motorDeRegras.avaliarEExecutar(10.0, clima, milho);

        assertTrue(alertasRecebidos.stream()
                .anyMatch(a -> a.getNivel() == NivelAlerta.EMERGENCIA));
    }

    @Test
    void deveSuspenderComPrevisaoDeChuvaForte() {
        DadosClimaticos clima = DadosClimaticos.builder()
                .temperatura(22.0).umidadeAr(85.0).velocidadeVento(3.0)
                .descricaoClima("chuva").previsaoChuva(true).volumeChuva(15.0).cidade("SP")
                .build();

        Irrigacao resultado = motorDeRegras.avaliarEExecutar(40.0, clima, milho);

        assertEquals(StatusIrrigacao.SUSPENSA, resultado.getStatus());
    }
}
