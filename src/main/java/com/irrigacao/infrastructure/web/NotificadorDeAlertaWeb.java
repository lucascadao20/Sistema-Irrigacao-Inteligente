package com.irrigacao.infrastructure.web;

import com.irrigacao.application.interfaces.NotificadorDeAlerta;
import com.irrigacao.domain.modelo.Alerta;

public class NotificadorDeAlertaWeb implements NotificadorDeAlerta {
    private final EstadoDoDashboard state;

    public NotificadorDeAlertaWeb(EstadoDoDashboard state) {
        this.state = state;
    }

    @Override
    public void notificar(Alerta alerta) {
        state.registrarAlerta(alerta);
    }
}
