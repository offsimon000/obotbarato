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
    public List<Cliente> listarManutencao(String busca) throws SQLException {
        util.Acesso.equipe(); List<Cliente> lista=new ArrayList<>();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT * FROM clientes WHERE nome ILIKE ? ORDER BY nome")){
            p.setString(1,"%"+busca.trim()+"%");try(ResultSet r=p.executeQuery()){while(r.next())lista.add(mapear(r));}
        }return lista;
    }
    public void reativar(int id) throws SQLException {
        util.Acesso.admin();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("UPDATE clientes SET ativo=true WHERE id=?")){
            p.setInt(1,id);if(p.executeUpdate()!=1)throw new SQLException("Cliente inexistente.");
        }
    }

    private static final Logger LOG = Logger.getLogger(ClienteDAO.class.getName());

    public List<Cliente> listarTodos() throws SQLException {
        util.Acesso.equipe();
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE ativo = true ORDER BY nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw e; }
        return lista;
    }

    public List<Cliente> buscarPorNome(String nome) throws SQLException {
        util.Acesso.equipe();
        List<Cliente> lista = new ArrayList<>();
        // PostgreSQL: ILIKE = case-insensitive LIKE
        String sql = "SELECT * FROM clientes WHERE nome ILIKE ? AND ativo = true ORDER BY nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nome.trim() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw e; }
        return lista;
    }

    public boolean salvar(Cliente c) throws SQLException {
        util.Acesso.equipe(); util.Validacao.cliente(c);
        String sql = "INSERT INTO clientes (nome, cpf, email, telefone, endereco, criado_por) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNome());
            ps.setString(2, opcional(c.getCpf()));
            ps.setString(3, c.getEmail());
            ps.setString(4, opcional(c.getTelefone()));
            ps.setString(5, opcional(c.getEndereco()));
            if (Sessao.getIdUsuario() > 0) ps.setInt(6, Sessao.getIdUsuario());
            else ps.setNull(6, java.sql.Types.INTEGER);
            if (ps.executeUpdate() != 1) throw new SQLException("Cliente não gravado.");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Cliente sem identificador.");
                c.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) { throw e; }
    }

    public boolean atualizar(Cliente c) throws SQLException {
        util.Acesso.equipe(); util.Validacao.cliente(c);
        String sql = "UPDATE clientes SET nome=?, cpf=?, email=?, telefone=?, endereco=? "
                   + "WHERE id=? AND ativo=true";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, opcional(c.getCpf()));
            ps.setString(3, c.getEmail());
            ps.setString(4, opcional(c.getTelefone()));
            ps.setString(5, opcional(c.getEndereco()));
            ps.setInt(6, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw e; }
    }

    public boolean excluir(int id) throws SQLException {
        util.Acesso.admin();
        // Soft delete: desativa em vez de apagar (preserva histórico de pedidos)
        String sql = "UPDATE clientes SET ativo = false WHERE id = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw e; }
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setCpf(rs.getString("cpf"));
        c.setEmail(rs.getString("email"));
        c.setTelefone(rs.getString("telefone"));
        c.setEndereco(rs.getString("endereco"));
        c.setAtivo(rs.getBoolean("ativo")); c.setAcessoLoja(rs.getString("senha")!=null);
        return c;
    }
}
