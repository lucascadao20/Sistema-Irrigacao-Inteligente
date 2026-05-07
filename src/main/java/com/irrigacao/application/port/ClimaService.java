package com.irrigacao.application.port;

import com.irrigacao.domain.model.DadosClimaticos;

public interface ClimaService {
    DadosClimaticos obterDados(String cidade, String pais);
}
