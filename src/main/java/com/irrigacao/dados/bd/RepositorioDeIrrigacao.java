package com.irrigacao.dados.bd;

import com.irrigacao.modelo.Irrigacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RepositorioDeIrrigacao {

    void salvar(Irrigacao irrigacao,
                double umidadeSolo,
                String estrategiaNome,
                LocalDateTime decididoEm);

    List<RegistroIrrigacao> listar(Optional<String> culturaNome,
                                   LocalDateTime inicio,
                                   LocalDateTime fim);

    ConsumoCultura consumoNoPeriodo(Optional<String> culturaNome,
                                    LocalDateTime inicio,
                                    LocalDateTime fim);
}
