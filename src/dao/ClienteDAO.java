package dao;

import model.Cliente;
import util.ConexaoBanco;
import util.Sessao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClienteDAO {

    private static final Logger LOG = Logger.getLogger(ClienteDAO.class.getName());

    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE ativo = true ORDER BY nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao listar clientes.", e);
        }
        return lista;
    }

    public List<Cliente> buscarPorNome(String nome) {
        List<Cliente> lista = new ArrayList<>();
        // PostgreSQL: ILIKE = case-insensitive LIKE
        String sql = "SELECT * FROM clientes WHERE nome ILIKE ? AND ativo = true ORDER BY nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nome.trim() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao buscar cliente por nome.", e);
        }
        return lista;
    }

    public boolean salvar(Cliente c) {
        String sql = "INSERT INTO clientes (nome, cpf, email, telefone, endereco, criado_por) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelefone());
            ps.setString(5, c.getEndereco());
            if (Sessao.getIdUsuario() > 0) ps.setInt(6, Sessao.getIdUsuario());
            else ps.setNull(6, java.sql.Types.INTEGER);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao salvar cliente.", e);
            return false;
        }
    }

    public boolean atualizar(Cliente c) {
        String sql = "UPDATE clientes SET nome=?, cpf=?, email=?, telefone=?, endereco=? "
                   + "WHERE id=?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelefone());
            ps.setString(5, c.getEndereco());
            ps.setInt(6, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao atualizar cliente.", e);
            return false;
        }
    }

    public boolean excluir(int id) {
        // Soft delete: desativa em vez de apagar (preserva histórico de pedidos)
        String sql = "UPDATE clientes SET ativo = false WHERE id = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Erro ao desativar cliente #" + id, e);
            return false;
        }
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