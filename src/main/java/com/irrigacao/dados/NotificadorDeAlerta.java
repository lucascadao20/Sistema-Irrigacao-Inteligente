package com.irrigacao.dados;

import com.irrigacao.modelo.Alerta;

public interface NotificadorDeAlerta {
    void notificar(Alerta alerta);
}
