package com.irrigacao.dados.bd;

import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

public final class ConexaoH2 {
    private static final Logger logger = LoggerFactory.getLogger(ConexaoH2.class);
    private static final AtomicLong contadorMemoria = new AtomicLong();

    private final JdbcConnectionPool pool;

    private ConexaoH2(String jdbcUrl) {
        this.pool = JdbcConnectionPool.create(jdbcUrl, "sa", "");
        inicializarSchema();
    }

    public static ConexaoH2 paraArquivo(Path diretorio) {
        Path arquivo = diretorio.resolve("irrigacao").toAbsolutePath();
        String url = "jdbc:h2:file:" + arquivo + ";MODE=LEGACY;DB_CLOSE_DELAY=-1";
        logger.info("Banco H2 em arquivo: {}.mv.db", arquivo);
        return new ConexaoH2(url);
    }

    public static ConexaoH2 emMemoria() {
        String nome = "test-" + contadorMemoria.incrementAndGet();
        String url = "jdbc:h2:mem:" + nome + ";DB_CLOSE_DELAY=-1";
        return new ConexaoH2(url);
    }

    public DataSource getDataSource() {
        return pool;
    }

    public void fechar() {
        pool.dispose();
    }

    private void inicializarSchema() {
        try (Connection conn = pool.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("RUNSCRIPT FROM 'classpath:schema.sql'");
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inicializar schema do H2", e);
        }
    }
}
