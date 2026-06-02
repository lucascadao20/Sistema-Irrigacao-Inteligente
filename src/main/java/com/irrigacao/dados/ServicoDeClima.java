package com.irrigacao.dados;

import com.irrigacao.modelo.DadosClimaticos;

public interface ServicoDeClima {
    DadosClimaticos obterDados(String cidade, String pais);
}
