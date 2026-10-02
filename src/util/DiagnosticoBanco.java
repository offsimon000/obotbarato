package util;

import dao.ClienteLojaDAO;
import dao.UsuarioDAO;
import java.sql.*;
import java.util.Arrays;

/** Verifica configuração e instalação sem abrir janelas nem alterar dados. */
public final class DiagnosticoBanco {
    private DiagnosticoBanco() {}
    public static void main(String[] args) {
        try {
            verificar();
            if (Arrays.asList(args).contains("--demo")) {
                if (new UsuarioDAO().autenticar("admin", "admin123") == null
                        || new UsuarioDAO().autenticar("vendedor", "vendedor123") == null
                        || new ClienteLojaDAO().autenticar("cliente@demo.local", "cliente123") == null)
                    throw new SQLException("Uma conta de demonstração não autentica; a senha pode ter sido alterada.");
                System.out.println("OK: contas de demonstração autenticadas.");
            }
        } catch (Exception e) {
            System.err.println("Diagnóstico: " + Erros.mensagem(e));
            System.exit(1);
        }
    }

    public static void verificar() throws SQLException {
        try (Connection con = ConexaoBanco.getConexao(); Statement st = con.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT current_database(), version()")) {
                rs.next();
                System.out.println("OK: conexão com banco " + rs.getString(1));
                System.out.println("Servidor: " + rs.getString(2).split(" on ")[0]);
            }
            String[] tabelas = {"schema_migrations", "usuarios", "clientes", "categorias", "produtos",
                    "estoque", "pedidos", "itens_pedido", "favoritos", "acessos_cliente", "movimentos_estoque"};
            for (String tabela : tabelas) {
                try (PreparedStatement ps = con.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
                    ps.setString(1, "public." + tabela);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (!rs.getBoolean(1)) throw new SQLException("Base não instalada. Execute o preparador de ambiente.");
                    }
                }
            }
            st.executeQuery("SELECT ativo FROM categorias LIMIT 0").close();
            st.executeQuery("SELECT imagem FROM produtos LIMIT 0").close();
            // Confere as colunas utilizadas pelos DAOs, sem consultar dados pessoais.
            st.executeQuery("SELECT criado_por FROM produtos LIMIT 0").close();
            st.executeQuery("SELECT criado_por FROM clientes LIMIT 0").close();
            st.executeQuery("SELECT registrado_por FROM pedidos LIMIT 0").close();
            System.out.println("OK: tabelas e colunas da base desktop disponíveis.");
        }
    }
}
