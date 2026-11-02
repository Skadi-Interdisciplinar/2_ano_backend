package skadi.api.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;

@Repository
@RequiredArgsConstructor
public class AccessLogRepository {
    private static final String INSERT_ACCESS_LOG =
            "INSERT INTO tb_log_acesso (cod_usuario, tentativa_sucesso, ip_origem, user_agent, motivo_falha) " +
            "VALUES (?, ?, CAST(? AS INET), ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public void insertAttempt(
            Long userId,
            boolean successful,
            String ipAddress,
            String userAgent,
            String failureReason
    ) {
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(INSERT_ACCESS_LOG);
            if (userId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, Math.toIntExact(userId));
            }
            statement.setBoolean(2, successful);
            statement.setString(3, ipAddress);
            statement.setString(4, userAgent);
            statement.setString(5, failureReason);
            return statement;
        });
    }
}
