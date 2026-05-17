package com.irrigacao.infrastructure.persistencia;

import com.irrigacao.application.interfaces.RepositorioDeCultura;
import com.irrigacao.domain.modelo.Cultura;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioDeCulturaEmMemoria implements RepositorioDeCultura {
    private final Map<String, Cultura> culturas = new HashMap<>();

    public RepositorioDeCulturaEmMemoria() {
        carregarDadosFAO();
    }

    @Override
    public Optional<Cultura> buscarPorNome(String nome) {
        return Optional.ofNullable(culturas.get(nome.toLowerCase()));
    }

    @Override
    public Map<String, Cultura> listarTodas() {
        return new HashMap<>(culturas);
    }

    private void carregarDadosFAO() {
        culturas.put("milho", new Cultura("Milho", 30, 60, 650, 1.15));
        culturas.put("soja", new Cultura("Soja", 35, 65, 550, 1.10));
        culturas.put("arroz", new Cultura("Arroz", 70, 90, 1500, 1.20));
        culturas.put("feijao", new Cultura("Feijao", 30, 55, 400, 1.05));
        culturas.put("trigo", new Cultura("Trigo", 25, 55, 500, 1.10));
        culturas.put("cafe", new Cultura("Cafe", 40, 70, 800, 0.95));
        culturas.put("cana", new Cultura("Cana-de-acucar", 45, 75, 1200, 1.25));
        culturas.put("algodao", new Cultura("Algodao", 30, 55, 700, 1.15));
        culturas.put("tomate", new Cultura("Tomate", 40, 70, 600, 1.05));
        culturas.put("alface", new Cultura("Alface", 50, 80, 300, 0.90));
    }
}
