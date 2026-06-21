package com.irrigacao.dados.bd;

import com.irrigacao.modelo.Irrigacao;
import com.irrigacao.modelo.StatusIrrigacao;
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
import java.util.Optional;

public class RepositorioDeIrrigacaoH2 implements RepositorioDeIrrigacao {
    private static final Logger logger = LoggerFactory.getLogger(RepositorioDeIrrigacaoH2.class);

    private static final String SQL_INSERT =
            "INSERT INTO irrigacao " +
            "(id, cultura_nome, status, volume_agua, motivo, estrategia_nome, umidade_solo, decidido_em) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_LISTAR_BASE =
            "SELECT id, cultura_nome, status, volume_agua, motivo, estrategia_nome, umidade_solo, decidido_em " +
            "FROM irrigacao " +
            "WHERE decidido_em BETWEEN ? AND ?";

    private static final String SQL_LISTAR_COM_CULTURA =
            SQL_LISTAR_BASE + " AND cultura_nome = ? ORDER BY decidido_em DESC";

    private static final String SQL_LISTAR_TODAS =
            SQL_LISTAR_BASE + " ORDER BY decidido_em DESC";

    private static final String SQL_CONSUMO_BASE =
            "SELECT COALESCE(SUM(volume_agua), 0) AS total, COUNT(*) AS qtd " +
            "FROM irrigacao " +
            "WHERE decidido_em BETWEEN ? AND ?";

    private static final String SQL_CONSUMO_COM_CULTURA = SQL_CONSUMO_BASE + " AND cultura_nome = ?";

    private final DataSource dataSource;

    public RepositorioDeIrrigacaoH2(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void salvar(Irrigacao irrigacao,
                       double umidadeSolo,
                       String estrategiaNome,
                       LocalDateTime decididoEm) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setString(1, irrigacao.getId());
            ps.setString(2, irrigacao.getCultura().getNome());
            ps.setString(3, irrigacao.getStatus().name());
            ps.setDouble(4, irrigacao.getVolumeAgua());
            ps.setString(5, truncar(irrigacao.getMotivo(), 500));
            if (estrategiaNome == null) {
                ps.setNull(6, java.sql.Types.VARCHAR);
            } else {
                ps.setString(6, estrategiaNome);
            }
            ps.setDouble(7, umidadeSolo);
            ps.setTimestamp(8, Timestamp.valueOf(decididoEm));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Falha ao salvar irrigacao {}: {}", irrigacao.getId(), e.getMessage());
        }
    }

    @Override
    public List<RegistroIrrigacao> listar(Optional<String> culturaNome,
                                          LocalDateTime inicio,
                                          LocalDateTime fim) {
        List<RegistroIrrigacao> resultado = new ArrayList<>();
        String sql = culturaNome.isPresent() ? SQL_LISTAR_COM_CULTURA : SQL_LISTAR_TODAS;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));
            if (culturaNome.isPresent()) {
                ps.setString(3, culturaNome.get());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new RegistroIrrigacao(
                            rs.getString("id"),
                            rs.getString("cultura_nome"),
                            StatusIrrigacao.valueOf(rs.getString("status")),
                            rs.getDouble("volume_agua"),
                            rs.getString("motivo"),
                            rs.getString("estrategia_nome"),
                            rs.getDouble("umidade_solo"),
                            rs.getTimestamp("decidido_em").toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            logger.warn("Falha ao listar irrigacoes: {}", e.getMessage());
        }
        return resultado;
    }

    @Override
    public ConsumoCultura consumoNoPeriodo(Optional<String> culturaNome,
                                           LocalDateTime inicio,
                                           LocalDateTime fim) {
        String sql = culturaNome.isPresent() ? SQL_CONSUMO_COM_CULTURA : SQL_CONSUMO_BASE;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));
            if (culturaNome.isPresent()) {
                ps.setString(3, culturaNome.get());
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ConsumoCultura(
                        culturaNome.orElse("(todas)"),
                        rs.getDouble("total"),
                        rs.getLong("qtd"));
            }
        } catch (SQLException e) {
            logger.warn("Falha ao calcular consumo: {}", e.getMessage());
            return new ConsumoCultura(culturaNome.orElse("(todas)"), 0.0, 0);
        }
    }

    private static String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
