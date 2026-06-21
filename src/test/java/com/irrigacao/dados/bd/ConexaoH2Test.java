package com.irrigacao.dados.bd;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoH2Test {

    @Test
    void devePoderObterConexaoEContemTabelasDoSchema() throws Exception {
        ConexaoH2 conexao = ConexaoH2.emMemoria();
        try (Connection c = conexao.getDataSource().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                     "WHERE TABLE_NAME IN ('LEITURA_SENSOR','IRRIGACAO')")) {
            rs.next();
            assertEquals(2, rs.getInt(1), "ambas as tabelas devem existir apos init");
        } finally {
            conexao.fechar();
        }
    }

    @Test
    void instanciasEmMemoriaDevemSerIsoladas() throws Exception {
        ConexaoH2 a = ConexaoH2.emMemoria();
        ConexaoH2 b = ConexaoH2.emMemoria();
        try (Connection ca = a.getDataSource().getConnection();
             Statement sa = ca.createStatement()) {
            sa.execute("INSERT INTO leitura_sensor(sensor_id,tipo,valor,valida,recebido_em) " +
                    "VALUES ('SU-001','UMIDADE_SOLO',42.0,TRUE,CURRENT_TIMESTAMP)");
        }
        try (Connection cb = b.getDataSource().getConnection();
             Statement sb = cb.createStatement();
             ResultSet rs = sb.executeQuery("SELECT COUNT(*) FROM leitura_sensor")) {
            rs.next();
            assertEquals(0, rs.getInt(1), "segundo banco em memoria nao deve ver inserts do primeiro");
        } finally {
            a.fechar();
            b.fechar();
        }
    }
}
