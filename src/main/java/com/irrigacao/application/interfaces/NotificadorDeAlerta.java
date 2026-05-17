package com.irrigacao.application.interfaces;

import com.irrigacao.domain.modelo.Alerta;

public interface NotificadorDeAlerta {
    void notificar(Alerta alerta);
}
