package com.irrigacao.negocio;

import com.irrigacao.dados.ServicoDeClima;
import com.irrigacao.dados.RepositorioDeCultura;
import com.irrigacao.dados.NotificadorDeAlerta;
import com.irrigacao.dados.GerenciadorDeSensores;
import com.irrigacao.dados.ProcessadorDeDados;
import com.irrigacao.dados.bd.ConexaoH2;
import com.irrigacao.dados.bd.RepositorioDeIrrigacaoH2;
import com.irrigacao.modelo.StatusIrrigacao;
import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.DadosClimaticos;
import com.irrigacao.modelo.Irrigacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServicoDeCicloIrrigacaoTest {

    private ServicoDeCicloIrrigacao cicloService;
    private ConexaoH2 conexao;
    private RepositorioDeIrrigacaoH2 repositorioIrrigacao;
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

        conexao = ConexaoH2.emMemoria();
        repositorioIrrigacao = new RepositorioDeIrrigacaoH2(conexao.getDataSource());

        cicloService = new ServicoDeCicloIrrigacao(
                servicoDeClima, repositorioDeCultura, motor, processador, gerenciador,
                repositorioIrrigacao, "Sao Paulo", "BR"
        );
    }

    @AfterEach
    void tearDown() {
        conexao.fechar();
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

    @Test
    void cicloExecutadoDevePersistirIrrigacaoNoBanco() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        cicloService.executarCiclo("milho", 22.0);
        cicloService.executarCiclo("milho", 55.0);

        var registros = repositorioIrrigacao.listar(
                Optional.of("Milho"), antes, LocalDateTime.now().plusSeconds(1));
        assertEquals(2, registros.size(),
                "ambos os ciclos (ativada e aguardando) devem ter sido persistidos");
    }
}
