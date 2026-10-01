package dao;

import model.Produto;
import util.ConexaoBanco;
import util.Sessao;
import util.Transacao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProdutoDAO {

    private static final Logger LOG = Logger.getLogger(ProdutoDAO.class.getName());

    public List<Produto> listarTodos() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        // PostgreSQL: sem diferença aqui — JOIN padrão funciona igual
        String sql = "SELECT p.*, c.nome AS nome_categoria "
                   + "FROM produtos p "
                   + "LEFT JOIN categorias c ON p.id_categoria = c.id "
                   + "WHERE p.ativo = true "
                   + "ORDER BY p.nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw e; }
        return lista;
    }

    public List<Produto> buscarPorNome(String nome) throws SQLException {
        List<Produto> lista = new ArrayList<>();
        // PostgreSQL: ILIKE para busca case-insensitive (MySQL usava LIKE que já ignora case)
        String sql = "SELECT p.*, c.nome AS nome_categoria "
                   + "FROM produtos p "
                   + "LEFT JOIN categorias c ON p.id_categoria = c.id "
                   + "WHERE p.ativo = true AND p.nome ILIKE ? "
                   + "ORDER BY p.nome";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nome.trim() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw e; }
        return lista;
    }

    public boolean salvar(Produto p) throws SQLException {
        validar(p);
        int id = Transacao.executar(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO produtos (nome, descricao, preco, id_categoria, ativo, criado_por) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getNome());
                ps.setString(2, p.getDescricao());
                ps.setBigDecimal(3, p.getPrecoDecimal());
                ps.setInt(4, p.getIdCategoria());
                ps.setBoolean(5, p.isAtivo());
                if (Sessao.getIdUsuario() > 0) ps.setInt(6, Sessao.getIdUsuario());
                else ps.setNull(6, Types.INTEGER);
                if (ps.executeUpdate() != 1) throw new SQLException("Produto não gravado.");
                try (ResultSet chave = ps.getGeneratedKeys()) {
                    if (!chave.next()) throw new SQLException("Produto sem identificador gerado.");
                    int idNovo = chave.getInt(1);
                    inserirEstoqueInicial(con, idNovo);
                    return idNovo;
                }
            }
        });
        p.setId(id);
        return true;
    }

    private static void validar(Produto p) {
        if (p.getNome() == null || p.getNome().isBlank() || p.getNome().length() > 120)
            throw new IllegalArgumentException("Informe um nome de produto com até 120 caracteres.");
        if (p.getIdCategoria() <= 0) throw new IllegalArgumentException("Selecione a categoria.");
    }

    public boolean atualizar(Produto p) throws SQLException {
        validar(p);
        String sql = "UPDATE produtos SET nome=?, descricao=?, preco=?, id_categoria=?, ativo=? "
                   + "WHERE id=?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNome());
            ps.setString(2, p.getDescricao());
            ps.setBigDecimal(3, p.getPrecoDecimal());
            ps.setInt(4, p.getIdCategoria());
            ps.setBoolean(5, p.isAtivo());
            ps.setInt(6, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Desativa produto (soft delete — nunca apaga do banco) */
    public boolean excluir(int id) throws SQLException {
        String sql = "UPDATE produtos SET ativo = false WHERE id = ?";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Privados ─────────────────────────────────────────────────────────

    private void inserirEstoqueInicial(Connection con, int idProduto) throws SQLException {
        String sql = "INSERT INTO estoque (id_produto, quantidade, estoque_minimo) "
                   + "VALUES (?, 0, 5)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProduto);
            if (ps.executeUpdate() != 1) throw new SQLException("Estoque inicial não gravado.");
        }
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setNome(rs.getString("nome"));
        p.setDescricao(rs.getString("descricao"));
        p.setPreco(rs.getBigDecimal("preco"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNomeCategoria(rs.getString("nome_categoria"));
        p.setAtivo(rs.getBoolean("ativo"));
        return p;
    }
}
