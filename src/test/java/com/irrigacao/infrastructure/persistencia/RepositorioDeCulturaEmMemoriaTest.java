package com.irrigacao.infrastructure.persistencia;

import com.irrigacao.modelo.Cultura;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioDeCulturaEmMemoriaTest {

    private final RepositorioDeCulturaEmMemoria repositorio = new RepositorioDeCulturaEmMemoria();

    @Test
    void deveBuscarCulturaPorNome() {
        Optional<Cultura> milho = repositorio.buscarPorNome("milho");
        assertTrue(milho.isPresent());
        assertEquals("Milho", milho.get().getNome());
    }

    @Test
    void deveBuscarCulturaIgnorandoCase() {
        Optional<Cultura> milho = repositorio.buscarPorNome("MILHO");
        assertTrue(milho.isPresent());
    }

    @Test
    void deveRetornarVazioParaCulturaInexistente() {
        Optional<Cultura> resultado = repositorio.buscarPorNome("banana");
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarTodasAsCulturas() {
        Map<String, Cultura> todas = repositorio.listarTodas();
        assertEquals(10, todas.size());
        assertTrue(todas.containsKey("milho"));
        assertTrue(todas.containsKey("soja"));
        assertTrue(todas.containsKey("arroz"));
        assertTrue(todas.containsKey("cafe"));
    }

    @Test
    void listarTodasDeveRetornarCopiaDefensiva() {
        Map<String, Cultura> todas = repositorio.listarTodas();
        todas.clear();
        assertEquals(10, repositorio.listarTodas().size());
    }
}
