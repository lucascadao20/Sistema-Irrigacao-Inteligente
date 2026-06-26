package com.irrigacao.dados.notificacao;

import com.irrigacao.dados.notificacao.NotificadorDeAlerta;
import com.irrigacao.modelo.NivelAlerta;
import com.irrigacao.modelo.Alerta;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificadorCompostoTest {

    @Test
    void deveNotificarTodosOsNotificadores() {
        List<Alerta> recebidos1 = new ArrayList<>();
        List<Alerta> recebidos2 = new ArrayList<>();

        NotificadorDeAlerta notificador1 = recebidos1::add;
        NotificadorDeAlerta notificador2 = recebidos2::add;

        NotificadorComposto composite = new NotificadorComposto(notificador1, notificador2);

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
        NotificadorComposto composite = new NotificadorComposto(recebidos::add);

        composite.notificar(new Alerta("A1", NivelAlerta.INFO, "info"));

        assertEquals(1, recebidos.size());
    }

    @Test
    void deveFuncionarSemNotificadores() {
        NotificadorComposto composite = new NotificadorComposto();
        assertDoesNotThrow(() -> composite.notificar(new Alerta("A1", NivelAlerta.INFO, "nada")));
    }
}
