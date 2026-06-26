package com.irrigacao.dados.notificacao;

import com.irrigacao.modelo.Alerta;

public interface NotificadorDeAlerta {
    void notificar(Alerta alerta);
}
