package com.irrigacao.infrastructure.notificacao;

import com.irrigacao.application.interfaces.NotificadorDeAlerta;
import com.irrigacao.domain.enums.NivelAlerta;
import com.irrigacao.domain.modelo.Alerta;

public class NotificadorDeAlertaConsole implements NotificadorDeAlerta {
    private final String nomeAgricultor;

    public NotificadorDeAlertaConsole(String nomeAgricultor) {
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
