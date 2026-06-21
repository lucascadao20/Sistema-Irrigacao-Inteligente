package com.irrigacao.dados.bd;

import com.irrigacao.modelo.Cultura;
import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.modelo.StatusIrrigacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioDeIrrigacaoH2Test {

    private ConexaoH2 conexao;
    private RepositorioDeIrrigacaoH2 repo;

    private final Cultura milho = new Cultura("Milho", 30, 60, 650, 1.15);
    private final Cultura soja  = new Cultura("Soja", 35, 65, 550, 1.10);

    @BeforeEach
    void setup() {
        conexao = ConexaoH2.emMemoria();
        repo = new RepositorioDeIrrigacaoH2(conexao.getDataSource());
    }

    @AfterEach
    void teardown() {
        conexao.fechar();
    }

    private Irrigacao novaIrrigacao(Cultura c, StatusIrrigacao status, double volume, String motivo) {
        return Irrigacao.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .cultura(c)
                .status(status)
                .volumeAgua(volume)
                .motivo(motivo)
                .build();
    }

    @Test
    void salvarPermiteListagemPosterior() {
        Irrigacao i = novaIrrigacao(milho, StatusIrrigacao.ATIVADA, 50.0, "irrigando");
        LocalDateTime t = LocalDateTime.of(2026, 6, 15, 10, 0);

        repo.salvar(i, 28.0, "Modo Seco", t);

        List<RegistroIrrigacao> regs = repo.listar(
                Optional.of("Milho"),
                t.minusMinutes(1),
                t.plusMinutes(1));
        assertEquals(1, regs.size());
        RegistroIrrigacao r = regs.get(0);
        assertEquals(i.getId(), r.id());
        assertEquals("Milho", r.culturaNome());
        assertEquals(StatusIrrigacao.ATIVADA, r.status());
        assertEquals(50.0, r.volumeAgua());
        assertEquals("Modo Seco", r.estrategiaNome());
        assertEquals(28.0, r.umidadeSolo());
    }

    @Test
    void listarFiltraPorCulturaQuandoInformada() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 15, 10, 0);
        repo.salvar(novaIrrigacao(milho, StatusIrrigacao.ATIVADA, 30.0, "a"), 25.0, "Seco", t);
        repo.salvar(novaIrrigacao(soja,  StatusIrrigacao.ATIVADA, 40.0, "b"), 25.0, "Seco", t.plusMinutes(1));

        assertEquals(1, repo.listar(Optional.of("Milho"), t.minusDays(1), t.plusDays(1)).size());
        assertEquals(1, repo.listar(Optional.of("Soja"),  t.minusDays(1), t.plusDays(1)).size());
        assertEquals(2, repo.listar(Optional.empty(),     t.minusDays(1), t.plusDays(1)).size());
    }

    @Test
    void consumoNoPeriodoSomaVolumesDaCultura() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 15, 10, 0);
        repo.salvar(novaIrrigacao(milho, StatusIrrigacao.ATIVADA, 30.0, "a"), 25.0, "Seco", t);
        repo.salvar(novaIrrigacao(milho, StatusIrrigacao.ATIVADA, 20.5, "b"), 26.0, "Seco", t.plusHours(1));
        repo.salvar(novaIrrigacao(soja,  StatusIrrigacao.ATIVADA, 99.0, "c"), 25.0, "Seco", t.plusHours(2));

        ConsumoCultura cMilho = repo.consumoNoPeriodo(
                Optional.of("Milho"), t.minusDays(1), t.plusDays(1));
        assertEquals(50.5, cMilho.volumeTotal(), 0.001);
        assertEquals(2L, cMilho.qtdIrrigacoes());
        assertEquals("Milho", cMilho.culturaNome());
    }

    @Test
    void consumoNoPeriodoSemCulturaSomaTodas() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 15, 10, 0);
        repo.salvar(novaIrrigacao(milho, StatusIrrigacao.ATIVADA, 30.0, "a"), 25.0, "Seco", t);
        repo.salvar(novaIrrigacao(soja,  StatusIrrigacao.SUSPENSA, 0.0,  "b"), 26.0, "Umido", t.plusHours(1));

        ConsumoCultura todas = repo.consumoNoPeriodo(
                Optional.empty(), t.minusDays(1), t.plusDays(1));
        assertEquals(30.0, todas.volumeTotal(), 0.001);
        assertEquals(2L, todas.qtdIrrigacoes());
        assertEquals("(todas)", todas.culturaNome());
    }

    @Test
    void consumoSemDadosRetornaZero() {
        ConsumoCultura c = repo.consumoNoPeriodo(
                Optional.of("Milho"),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now());
        assertEquals(0.0, c.volumeTotal());
        assertEquals(0L, c.qtdIrrigacoes());
    }

    @Test
    void salvarPermiteEstrategiaNula() {
        Irrigacao i = novaIrrigacao(milho, StatusIrrigacao.AGUARDANDO, 0.0, "umidade adequada");
        LocalDateTime t = LocalDateTime.of(2026, 6, 15, 10, 0);

        repo.salvar(i, 55.0, null, t);

        RegistroIrrigacao r = repo.listar(
                Optional.of("Milho"), t.minusMinutes(1), t.plusMinutes(1)).get(0);
        assertNull(r.estrategiaNome());
    }
}
