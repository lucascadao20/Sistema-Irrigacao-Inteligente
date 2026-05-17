package com.irrigacao.application.interfaces;

import com.irrigacao.domain.modelo.Cultura;
import java.util.Map;
import java.util.Optional;

public interface RepositorioDeCultura {
    Optional<Cultura> buscarPorNome(String nome);
    Map<String, Cultura> listarTodas();
}
