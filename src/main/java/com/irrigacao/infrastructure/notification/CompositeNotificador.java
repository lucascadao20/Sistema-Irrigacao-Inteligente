package com.irrigacao.infrastructure.notification;

import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.model.Alerta;

import java.util.List;

public class CompositeNotificador implements NotificadorAlerta {
    private final List<NotificadorAlerta> notificadores;

    public CompositeNotificador(NotificadorAlerta... notificadores) {
        this.notificadores = List.of(notificadores);
    }

    @Override
    public void notificar(Alerta alerta) {
        notificadores.forEach(n -> n.notificar(alerta));
    }
}
