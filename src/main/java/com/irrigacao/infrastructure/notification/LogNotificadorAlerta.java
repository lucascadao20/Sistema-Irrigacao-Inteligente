package com.irrigacao.infrastructure.notification;

import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.model.Alerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogNotificadorAlerta implements NotificadorAlerta {
    private static final Logger logger = LoggerFactory.getLogger(LogNotificadorAlerta.class);

    @Override
    public void notificar(Alerta alerta) {
        switch (alerta.getNivel()) {
            case INFO -> logger.info(alerta.getMensagem());
            case AVISO -> logger.warn(alerta.getMensagem());
            case CRITICO, EMERGENCIA -> logger.error(alerta.getMensagem());
        }
    }
}
