package com.irrigacao.ui.web;

import com.irrigacao.modelo.Alerta;
import com.irrigacao.modelo.NivelAlerta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificadorDeAlertaWebTest {

    @Test
    void notificarRegistraAlertaNoEstado() {
        EstadoDoDashboard estado = new EstadoDoDashboard();
        NotificadorDeAlertaWeb notificador = new NotificadorDeAlertaWeb(estado);

        notificador.notificar(new Alerta("A1", NivelAlerta.CRITICO, "irrigacao ativada"));

        assertEquals(1, estado.getAlertas().size());
        assertEquals("irrigacao ativada", estado.getAlertas().get(0).getMensagem());
        assertEquals(NivelAlerta.CRITICO, estado.getAlertas().get(0).getNivel());
    }
}
