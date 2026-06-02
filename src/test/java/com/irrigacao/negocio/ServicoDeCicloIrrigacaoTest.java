package com.irrigacao.negocio;

import com.irrigacao.negocio.FabricaDeEstrategia;
import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.dados.RepositorioDeCultura;
import com.irrigacao.dados.NotificadorDeAlerta;
import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.dados.ProcessadorDeDados;
import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServicoDeCicloIrrigacaoTest {

    private ServicoDeCicloIrrigacao cicloService;
    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);

    @BeforeEach
    void setUp() {
        ServicoDeClima servicoDeClima = (cidade, pais) -> DadosClimaticos.builder()
                .temperatura(28.0).umidadeAr(50.0).velocidadeVento(3.0)
                .descricaoClima("limpo").previsaoChuva(false).volumeChuva(0).cidade(cidade)
                .build();

        RepositorioDeCultura repositorioDeCultura = new RepositorioDeCultura() {
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

        NotificadorDeAlerta notificador = alerta -> {};
        MotorDeRegras motor = new MotorDeRegras(new FabricaDeEstrategia(), notificador);
        GerenciadorDeSensores gerenciador = new GerenciadorDeSensores();
        ProcessadorDeDados processador = new ProcessadorDeDados(gerenciador);

        cicloService = new ServicoDeCicloIrrigacao(
                servicoDeClima, repositorioDeCultura, motor, processador, gerenciador, "Sao Paulo", "BR"
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
