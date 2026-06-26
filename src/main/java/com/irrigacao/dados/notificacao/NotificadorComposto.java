package com.irrigacao.dados.notificacao;

import com.irrigacao.dados.notificacao.NotificadorDeAlerta;
import com.irrigacao.modelo.Alerta;

import java.util.List;

public class NotificadorComposto implements NotificadorDeAlerta {
    private final List<NotificadorDeAlerta> notificadores;

    public NotificadorComposto(NotificadorDeAlerta... notificadores) {
        this.notificadores = List.of(notificadores);
    }

    @Override
    public void notificar(Alerta alerta) {
        notificadores.forEach(n -> n.notificar(alerta));
    }
}
