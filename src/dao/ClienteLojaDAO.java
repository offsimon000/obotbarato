package dao;

import model.Cliente;
import util.ConexaoBanco;
import util.SenhaCripto;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClienteLojaDAO {

    private static final Logger LOG = Logger.getLogger(ClienteLojaDAO.class.getName());

    // ── Autenticação ─────────────────────────────────────────────────────
    public Cliente autenticar(String email, String senhaPlana) {
        if (email == null || senhaPlana == null
                || email.isBlank() || senhaPlana.isBlank()) return null;

        String sql = "SELECT * FROM clientes WHERE email = ? AND ativo = true";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("senha");
                if (!SenhaCripto.verificar(senhaPlana, hash)) return null;
                Cliente c = mapear(rs);
                if (SenhaCripto.ehFormatoLegado(hash)) migrarSenha(c.getId(), senhaPlana);
                return c;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao autenticar cliente.", e);
        }
        return null;
    }

    // ── Cadastro ─────────────────────────────────────────────────────────
    /** Retorna null se sucesso, ou mensagem de erro. */
    public String cadastrar(String nome, String cpf, String email,
                             String telefone, String endereco, String senhaPlana) {
        String erroVal = validar(nome, email, senhaPlana);
        if (erroVal != null) return erroVal;
        if (emailExiste(email.trim().toLowerCase())) return "Este email ja esta cadastrado.";

        String sql = "INSERT INTO clientes (nome, cpf, email, telefone, endereco, senha, ativo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, true)";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.setString(2, cpf.trim());
            ps.setString(3, email.trim().toLowerCase());
            ps.setString(4, telefone.trim());
            ps.setString(5, endereco.trim());
            ps.setString(6, SenhaCripto.hashear(senhaPlana));
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao cadastrar cliente.", e);
            return "Erro interno. Tente novamente.";
        }
    }

    public boolean emailExiste(String email) {
        String sql = "SELECT 1 FROM clientes WHERE email = ? LIMIT 1";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erro ao verificar email.", e);
            return false;
        }
    }

    // ── Esqueci minha senha ───────────────────────────────────────────────

    /**
     * Verifica se existe cliente com esse email E cpf.
     * Usado na tela "Esqueci minha senha" como segundo fator.
     */
    public boolean verificarEmailCpf(String email, String cpf) {
        String sql = "SELECT 1 FROM clientes WHERE email = ? AND cpf = ? AND ativo = true LIMIT 1";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, cpf.trim());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erro ao verificar email+cpf.", e);
            return false;
        }
    }

    /**
     * Redefine a senha do cliente pelo email.
     * Só chame após verificarEmailCpf retornar true.
     */
    public boolean redefinirSenha(String email, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 6) return false;
        String sql = "UPDATE clientes SET senha = ? WHERE email = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, SenhaCripto.hashear(novaSenha));
            ps.setString(2, email.trim().toLowerCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao redefinir senha.", e);
            return false;
        }
    }

    // ── Favoritos ────────────────────────────────────────────────────────

    /** Uma única query — use isso na galeria, não ehFavorito() em loop */
    public Set<Integer> listarFavoritosSet(int idCliente) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT id_produto FROM favoritos WHERE id_cliente = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_produto"));
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erro ao listar favoritos.", e);
        }
        return ids;
    }

    public boolean adicionarFavorito(int idCliente, int idProduto) {
        String sql = "INSERT INTO favoritos (id_cliente, id_produto) VALUES (?, ?) "
                   + "ON CONFLICT (id_cliente, id_produto) DO NOTHING";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente); ps.setInt(2, idProduto);
            ps.executeUpdate(); return true;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erro ao adicionar favorito.", e); return false;
        }
    }

    public boolean removerFavorito(int idCliente, int idProduto) {
        String sql = "DELETE FROM favoritos WHERE id_cliente = ? AND id_produto = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente); ps.setInt(2, idProduto);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Erro ao remover favorito.", e); return false;
        }
    }

    // ── Privados ─────────────────────────────────────────────────────────

    private void migrarSenha(int id, String senhaPlana) {
        String sql = "UPDATE clientes SET senha = ? WHERE id = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, SenhaCripto.hashear(senhaPlana));
            ps.setInt(2, id);
            ps.executeUpdate();
            LOG.info("Senha migrada MD5->SHA-256 para cliente #" + id);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Falha ao migrar senha do cliente #" + id, e);
        }
    }

    private String validar(String nome, String email, String senha) {
        if (nome  == null || nome.trim().length()  < 3) return "Nome deve ter ao menos 3 caracteres.";
        if (email == null || !email.contains("@"))      return "Email invalido.";
        if (senha == null || senha.length()        < 6) return "Senha deve ter ao menos 6 caracteres.";
        if (nome.trim().length()  > 120)                return "Nome muito longo.";
        if (email.trim().length() > 150)                return "Email muito longo.";
        if (senha.length()        > 128)                return "Senha muito longa.";
        return null;
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setCpf(rs.getString("cpf"));
        c.setEmail(rs.getString("email"));
        c.setTelefone(rs.getString("telefone"));
        c.setEndereco(rs.getString("endereco"));
        return c;
    }
}