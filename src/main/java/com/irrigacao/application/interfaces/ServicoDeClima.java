package com.irrigacao.application.interfaces;

import com.irrigacao.domain.modelo.DadosClimaticos;

public interface ServicoDeClima {
    DadosClimaticos obterDados(String cidade, String pais);
}
