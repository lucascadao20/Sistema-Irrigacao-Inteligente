package com.irrigacao.application.port;

import com.irrigacao.domain.model.Alerta;

public interface NotificadorAlerta {
    void notificar(Alerta alerta);
}
