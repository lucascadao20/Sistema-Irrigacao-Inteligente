package com.irrigacao.infrastructure.notification;

import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.enums.NivelAlerta;
import com.irrigacao.domain.model.Alerta;

public class ConsoleNotificadorAlerta implements NotificadorAlerta {
    private final String nomeAgricultor;

    public ConsoleNotificadorAlerta(String nomeAgricultor) {
        this.nomeAgricultor = nomeAgricultor;
    }

    @Override
    public void notificar(Alerta alerta) {
        String prefixo = getPrefixo(alerta.getNivel());
        System.out.printf("   %s [NOTIFICACAO para %s] %s%n",
                prefixo, nomeAgricultor, alerta.getMensagem());
    }

    private String getPrefixo(NivelAlerta nivel) {
        return switch (nivel) {
            case INFO -> "[INFO]";
            case AVISO -> "[AVISO]";
            case CRITICO -> "[CRITICO]";
            case EMERGENCIA -> "[EMERGENCIA]";
        };
    }
}
