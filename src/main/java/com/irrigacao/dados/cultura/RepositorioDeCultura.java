package com.irrigacao.dados.cultura;

import com.irrigacao.modelo.Cultura;
import java.util.Map;
import java.util.Optional;

public interface RepositorioDeCultura {
    Optional<Cultura> buscarPorNome(String nome);
    Map<String, Cultura> listarTodas();
}
