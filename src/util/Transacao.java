package util;

import java.sql.Connection;
import java.sql.SQLException;

public final class Transacao {
    private Transacao() {}
    @FunctionalInterface
    public interface Operacao<T> {
        T executar(Connection con) throws SQLException;
    }

    public static <T> T executar(Operacao<T> operacao) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacao.executar(con);
                con.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); }
                catch (SQLException falhaRollback) { e.addSuppressed(falhaRollback); }
                throw e;
            }
        }
    }
}
