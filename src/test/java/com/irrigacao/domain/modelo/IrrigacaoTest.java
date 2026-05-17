package com.irrigacao.domain.modelo;

import com.irrigacao.domain.enums.StatusIrrigacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IrrigacaoTest {

    private Cultura criarMilho() {
        return new Cultura("Milho", 30, 60, 650, 1.15);
    }

    @Test
    void builderDeveCriarIrrigacaoComTodosOsCampos() {
        Cultura milho = criarMilho();
        Irrigacao irrigacao = Irrigacao.builder()
                .id("IR-001")
                .cultura(milho)
                .status(StatusIrrigacao.ATIVADA)
                .volumeAgua(45.5)
                .motivo("Solo seco")
                .build();

        assertEquals("IR-001", irrigacao.getId());
        assertEquals(milho, irrigacao.getCultura());
        assertEquals(StatusIrrigacao.ATIVADA, irrigacao.getStatus());
        assertEquals(45.5, irrigacao.getVolumeAgua());
        assertEquals("Solo seco", irrigacao.getMotivo());
        assertNotNull(irrigacao.getInicio());
    }

    @Test
    void builderDeveCriarIrrigacaoAguardando() {
        Irrigacao irrigacao = Irrigacao.builder()
                .id("IR-002")
                .cultura(criarMilho())
                .status(StatusIrrigacao.AGUARDANDO)
                .volumeAgua(0)
                .motivo("Umidade adequada")
                .build();

        assertEquals(StatusIrrigacao.AGUARDANDO, irrigacao.getStatus());
        assertEquals(0, irrigacao.getVolumeAgua());
    }

    @Test
    void devePermitirAlterarStatus() {
        Irrigacao irrigacao = Irrigacao.builder()
                .id("IR-003")
                .cultura(criarMilho())
                .status(StatusIrrigacao.ATIVADA)
                .volumeAgua(20.0)
                .motivo("teste")
                .build();

        irrigacao.setStatus(StatusIrrigacao.SUSPENSA);
        assertEquals(StatusIrrigacao.SUSPENSA, irrigacao.getStatus());
    }
}
