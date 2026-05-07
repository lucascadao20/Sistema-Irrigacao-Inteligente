package com.irrigacao.infrastructure.notification;

import com.irrigacao.application.port.NotificadorAlerta;
import com.irrigacao.domain.enums.NivelAlerta;
import com.irrigacao.domain.model.Alerta;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeNotificadorTest {

    @Test
    void deveNotificarTodosOsNotificadores() {
        List<Alerta> recebidos1 = new ArrayList<>();
        List<Alerta> recebidos2 = new ArrayList<>();

        NotificadorAlerta notificador1 = recebidos1::add;
        NotificadorAlerta notificador2 = recebidos2::add;

        CompositeNotificador composite = new CompositeNotificador(notificador1, notificador2);

        Alerta alerta = new Alerta("A1", NivelAlerta.AVISO, "teste");
        composite.notificar(alerta);

        assertEquals(1, recebidos1.size());
        assertEquals(1, recebidos2.size());
        assertEquals("teste", recebidos1.get(0).getMensagem());
        assertEquals("teste", recebidos2.get(0).getMensagem());
    }

    @Test
    void deveFuncionarComNotificadorUnico() {
        List<Alerta> recebidos = new ArrayList<>();
        CompositeNotificador composite = new CompositeNotificador(recebidos::add);

        composite.notificar(new Alerta("A1", NivelAlerta.INFO, "info"));

        assertEquals(1, recebidos.size());
    }

    @Test
    void deveFuncionarSemNotificadores() {
        CompositeNotificador composite = new CompositeNotificador();
        assertDoesNotThrow(() -> composite.notificar(new Alerta("A1", NivelAlerta.INFO, "nada")));
    }
}
