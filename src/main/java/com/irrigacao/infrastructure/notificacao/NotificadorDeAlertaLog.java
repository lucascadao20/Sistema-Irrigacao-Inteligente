package com.irrigacao.infrastructure.notificacao;

import com.irrigacao.application.interfaces.NotificadorDeAlerta;
import com.irrigacao.modelo.Alerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificadorDeAlertaLog implements NotificadorDeAlerta {
    private static final Logger logger = LoggerFactory.getLogger(NotificadorDeAlertaLog.class);

    @Override
    public void notificar(Alerta alerta) {
        switch (alerta.getNivel()) {
            case INFO -> logger.info(alerta.getMensagem());
            case AVISO -> logger.warn(alerta.getMensagem());
            case CRITICO, EMERGENCIA -> logger.error(alerta.getMensagem());
        }
    }
}
