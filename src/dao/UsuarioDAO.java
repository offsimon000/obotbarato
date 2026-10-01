package dao;

import model.Usuario;
import util.ConexaoBanco;
import util.SenhaCripto;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO de usuários administrativos.
 *
 * MELHORIA: Substituído MD5 por SHA-256 + salt.
 * Migração automática: na próxima vez que o admin fizer login,
 * o hash é atualizado silenciosamente para SHA-256.
 */
public class UsuarioDAO {

    private static final Logger LOG = Logger.getLogger(UsuarioDAO.class.getName());

    /**
     * Autentica usuário admin/vendedor.
     * Aceita tanto o formato legado (MD5) quanto o novo (SHA-256+salt).
     */
    public Usuario autenticar(String login, String senhaPlana) throws SQLException {
        if (login == null || senhaPlana == null
                || login.isBlank() || senhaPlana.isBlank()) return null;

        String sql = "SELECT * FROM usuarios WHERE login = ? AND ativo = true";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, login.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String hashArmazenado = rs.getString("senha");

                if (!SenhaCripto.verificar(senhaPlana, hashArmazenado)) return null;

                Usuario usuario = mapear(rs);

                // Migra hash legado MD5 → SHA-256 de forma transparente
                if (SenhaCripto.ehFormatoLegado(hashArmazenado)) {
                    migrarSenha(usuario.getId(), senhaPlana);
                }
                return usuario;
            }
        } catch (SQLException e) { throw e; }
        return null;
    }

    /** Salva novo usuário com SHA-256 */
    public boolean salvar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios (nome, login, senha, perfil, ativo) VALUES (?,?,?,?,true)";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getLogin());
            ps.setString(3, SenhaCripto.hashear(u.getSenha()));
            ps.setString(4, u.getPerfil());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw e; }
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVADOS
    // ═══════════════════════════════════════════════════════════════

    private void migrarSenha(int idUsuario, String senhaPlana) {
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, SenhaCripto.hashear(senhaPlana));
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
            LOG.info("Senha migrada MD5 -> SHA-256 para usuario #" + idUsuario);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Nao foi possivel migrar senha do usuario #" + idUsuario, e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setLogin(rs.getString("login"));
        u.setSenha(rs.getString("senha"));
        u.setPerfil(rs.getString("perfil"));
        return u;
    }
}
