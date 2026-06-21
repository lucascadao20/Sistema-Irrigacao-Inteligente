package com.irrigacao.dados.bd;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioDeLeituraSensorH2Test {

    private ConexaoH2 conexao;
    private RepositorioDeLeituraSensorH2 repo;

    @BeforeEach
    void setup() {
        conexao = ConexaoH2.emMemoria();
        repo = new RepositorioDeLeituraSensorH2(conexao.getDataSource());
    }

    @AfterEach
    void teardown() {
        conexao.fechar();
    }

    @Test
    void salvarPersisteContagemSobeUm() {
        Sensor s = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A");
        LeituraSensor l = new LeituraSensor(s, 42.5, LocalDateTime.now());

        repo.salvar(l);

        assertEquals(1, repo.contar());
    }

    @Test
    void listarRetornaApenasLeiturasDoTipoEPeriodoSolicitados() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 15, 10, 0);

        Sensor solo = new Sensor("SU-001", TipoSensor.UMIDADE_SOLO, "Talhao A");
        Sensor temp = new Sensor("ST-001", TipoSensor.TEMPERATURA, "Talhao A");

        repo.salvar(new LeituraSensor(solo, 40.0, base));
        repo.salvar(new LeituraSensor(solo, 50.0, base.plusHours(1)));
        repo.salvar(new LeituraSensor(temp, 25.0, base.plusMinutes(30)));
        repo.salvar(new LeituraSensor(solo, 60.0, base.plusDays(2)));

        List<LeituraSensor> soloNasDuasPrimeirasHoras = repo.listar(
                TipoSensor.UMIDADE_SOLO,
                base.minusMinutes(1),
                base.plusHours(2));

        assertEquals(2, soloNasDuasPrimeirasHoras.size());
        assertEquals(TipoSensor.UMIDADE_SOLO, soloNasDuasPrimeirasHoras.get(0).getSensor().getTipo());
        assertEquals(40.0, soloNasDuasPrimeirasHoras.get(0).getValor());
        assertEquals(50.0, soloNasDuasPrimeirasHoras.get(1).getValor());
    }

    @Test
    void listarRetornaVazioQuandoTipoNaoExiste() {
        Sensor s = new Sensor("SP-001", TipoSensor.PH_SOLO, "Talhao A");
        repo.salvar(new LeituraSensor(s, 6.5, LocalDateTime.now()));

        List<LeituraSensor> res = repo.listar(
                TipoSensor.UMIDADE_AR,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1));

        assertTrue(res.isEmpty());
    }
}
