package com.irrigacao.infrastructure.persistence;

import com.irrigacao.domain.model.Cultura;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCulturaRepositoryTest {

    private final InMemoryCulturaRepository repository = new InMemoryCulturaRepository();

    @Test
    void deveBuscarCulturaPorNome() {
        Optional<Cultura> milho = repository.buscarPorNome("milho");
        assertTrue(milho.isPresent());
        assertEquals("Milho", milho.get().getNome());
    }

    @Test
    void deveBuscarCulturaIgnorandoCase() {
        Optional<Cultura> milho = repository.buscarPorNome("MILHO");
        assertTrue(milho.isPresent());
    }

    @Test
    void deveRetornarVazioParaCulturaInexistente() {
        Optional<Cultura> resultado = repository.buscarPorNome("banana");
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarTodasAsCulturas() {
        Map<String, Cultura> todas = repository.listarTodas();
        assertEquals(10, todas.size());
        assertTrue(todas.containsKey("milho"));
        assertTrue(todas.containsKey("soja"));
        assertTrue(todas.containsKey("arroz"));
        assertTrue(todas.containsKey("cafe"));
    }

    @Test
    void listarTodasDeveRetornarCopiaDefensiva() {
        Map<String, Cultura> todas = repository.listarTodas();
        todas.clear();
        assertEquals(10, repository.listarTodas().size());
    }
}
