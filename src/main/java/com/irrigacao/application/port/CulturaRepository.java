package com.irrigacao.application.port;

import com.irrigacao.domain.model.Cultura;
import java.util.Map;
import java.util.Optional;

public interface CulturaRepository {
    Optional<Cultura> buscarPorNome(String nome);
    Map<String, Cultura> listarTodas();
}
