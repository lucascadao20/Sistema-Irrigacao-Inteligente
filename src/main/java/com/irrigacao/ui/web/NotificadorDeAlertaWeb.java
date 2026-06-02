package com.irrigacao.ui.web;

import com.irrigacao.dados.NotificadorDeAlerta;
import com.irrigacao.modelo.Alerta;

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
