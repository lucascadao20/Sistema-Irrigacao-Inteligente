package com.irrigacao.application.service;

import com.irrigacao.application.factory.StrategyFactory;
import com.irrigacao.application.port.ClimaService;
import com.irrigacao.application.port.CulturaRepository;
import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.enums.StatusIrrigacao;
import com.irrigacao.domain.model.Cultura;
import com.irrigacao.domain.model.DadosClimaticos;
import com.irrigacao.domain.model.Irrigacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CicloIrrigacaoServiceTest {

    private CicloIrrigacaoService cicloService;
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @BeforeEach
    void setUp() {
        ClimaService climaService = (cidade, pais) -> DadosClimaticos.builder()
                .temperatura(28.0).umidadeAr(50.0).velocidadeVento(3.0)
                .descricaoClima("limpo").previsaoChuva(false).volumeChuva(0).cidade(cidade)
                .build();

        CulturaRepository culturaRepo = new CulturaRepository() {
            @Override
            public Optional<Cultura> buscarPorNome(String nome) {
                if ("milho".equals(nome)) return Optional.of(milho);
                return Optional.empty();
            }
            @Override
            public Map<String, Cultura> listarTodas() {
                return Map.of("milho", milho);
            }
        };

        NotificadorAlerta notificador = alerta -> {};
        MotorRegras motor = new MotorRegras(new StrategyFactory(), notificador);
        GerenciadorSensores gerenciador = new GerenciadorSensores();
        ProcessadorDados processador = new ProcessadorDados(gerenciador);

        cicloService = new CicloIrrigacaoService(
                climaService, culturaRepo, motor, processador, gerenciador, "Sao Paulo", "BR"
        );
    }

    @Test
    void deveExecutarCicloComUmidadeBaixa() {
        Irrigacao resultado = cicloService.executarCiclo("milho", 22.0);

        assertNotNull(resultado);
        assertEquals(StatusIrrigacao.ATIVADA, resultado.getStatus());
        assertTrue(resultado.getVolumeAgua() > 0);
    }

    @Test
    void deveExecutarCicloComUmidadeAdequada() {
        Irrigacao resultado = cicloService.executarCiclo("milho", 55.0);

        assertNotNull(resultado);
        assertEquals(StatusIrrigacao.AGUARDANDO, resultado.getStatus());
    }

    @Test
    void deveLancarExcecaoParaCulturaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> cicloService.executarCiclo("banana", 50.0));
    }

    @Test
    void deveListarCulturas() {
        Map<String, Cultura> culturas = cicloService.listarCulturas();
        assertEquals(1, culturas.size());
        assertTrue(culturas.containsKey("milho"));
    }
}
