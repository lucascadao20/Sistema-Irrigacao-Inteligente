package com.irrigacao.presentation.web;

import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.model.Alerta;

public class WebNotificadorAlerta implements NotificadorAlerta {
    private final DashboardState state;

    public WebNotificadorAlerta(DashboardState state) {
        this.state = state;
    }

    @Override
    public void notificar(Alerta alerta) {
        state.registrarAlerta(alerta);
    }
}
