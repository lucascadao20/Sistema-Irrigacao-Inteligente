package com.irrigacao.dados.bd;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.TipoSensor;

import java.time.LocalDateTime;
import java.util.List;

public interface RepositorioDeLeituraSensor {
    void salvar(LeituraSensor leitura);
    List<LeituraSensor> listar(TipoSensor tipo, LocalDateTime inicio, LocalDateTime fim);
    long contar();
}
