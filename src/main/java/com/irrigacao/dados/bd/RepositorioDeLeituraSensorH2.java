package com.irrigacao.dados.bd;

import com.irrigacao.modelo.LeituraSensor;
import com.irrigacao.modelo.Sensor;
import com.irrigacao.modelo.TipoSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RepositorioDeLeituraSensorH2 implements RepositorioDeLeituraSensor {
    private static final Logger logger = LoggerFactory.getLogger(RepositorioDeLeituraSensorH2.class);

    private static final String SQL_INSERT =
            "INSERT INTO leitura_sensor (sensor_id, tipo, valor, valida, recebido_em) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_LISTAR =
            "SELECT sensor_id, tipo, valor, valida, recebido_em " +
            "FROM leitura_sensor " +
            "WHERE tipo = ? AND recebido_em BETWEEN ? AND ? " +
            "ORDER BY recebido_em ASC";

    private static final String SQL_CONTAR = "SELECT COUNT(*) FROM leitura_sensor";

    private final DataSource dataSource;

    public RepositorioDeLeituraSensorH2(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void salvar(LeituraSensor leitura) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setString(1, leitura.getSensor().getId());
            ps.setString(2, leitura.getSensor().getTipo().name());
            ps.setDouble(3, leitura.getValor());
            ps.setBoolean(4, leitura.isValida());
            ps.setTimestamp(5, Timestamp.valueOf(leitura.getTimestamp()));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Falha ao salvar leitura {}: {}", leitura, e.getMessage());
        }
    }

    @Override
    public List<LeituraSensor> listar(TipoSensor tipo, LocalDateTime inicio, LocalDateTime fim) {
        List<LeituraSensor> resultado = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_LISTAR)) {
            ps.setString(1, tipo.name());
            ps.setTimestamp(2, Timestamp.valueOf(inicio));
            ps.setTimestamp(3, Timestamp.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sensor sensor = new Sensor(
                            rs.getString("sensor_id"),
                            TipoSensor.valueOf(rs.getString("tipo")),
                            "(historico)");
                    LeituraSensor leitura = new LeituraSensor(
                            sensor,
                            rs.getDouble("valor"),
                            rs.getTimestamp("recebido_em").toLocalDateTime());
                    resultado.add(leitura);
                }
            }
        } catch (SQLException e) {
            logger.warn("Falha ao listar leituras: {}", e.getMessage());
        }
        return resultado;
    }

    @Override
    public long contar() {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_CONTAR);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            logger.warn("Falha ao contar leituras: {}", e.getMessage());
            return 0;
        }
    }
}
