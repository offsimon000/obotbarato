package dao;

import util.ConexaoBanco;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {
    public int getQuantidade(int idProduto) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT quantidade FROM estoque WHERE id_produto = ?")) {
            ps.setInt(1, idProduto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("quantidade") : 0;
            }
        }
    }

    public void atualizar(int idProduto, int quantidade) throws SQLException {
        if (quantidade < 0) throw new IllegalArgumentException("Estoque não pode ser negativo.");
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement("UPDATE estoque SET quantidade = ? WHERE id_produto = ?")) {
            ps.setInt(1, quantidade);
            ps.setInt(2, idProduto);
            if (ps.executeUpdate() != 1) throw new SQLException("Produto sem registro de estoque.");
        }
    }

    public void entrada(int idProduto, int qtd) throws SQLException {
        validarQuantidade(qtd);
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement("UPDATE estoque SET quantidade = quantidade + ? WHERE id_produto = ?")) {
            ps.setInt(1, qtd);
            ps.setInt(2, idProduto);
            if (ps.executeUpdate() != 1) throw new SQLException("Produto sem registro de estoque.");
        }
    }

    public void saida(int idProduto, int qtd) throws SQLException {
        try (Connection con = ConexaoBanco.getConexao()) { saida(con, idProduto, qtd); }
    }

    /** Participa da transação do pedido; não fecha a conexão recebida. */
    public void saida(Connection con, int idProduto, int qtd) throws SQLException {
        validarQuantidade(qtd);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE estoque SET quantidade = quantidade - ? WHERE id_produto = ? AND quantidade >= ?")) {
            ps.setInt(1, qtd);
            ps.setInt(2, idProduto);
            ps.setInt(3, qtd);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Estoque insuficiente para o produto ID: " + idProduto);
        }
    }

    private static void validarQuantidade(int qtd) {
        if (qtd <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
    }

    public List<String[]> listarEstoqueBaixo() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT p.nome, e.quantidade, e.estoque_minimo FROM estoque e "
                + "JOIN produtos p ON e.id_produto = p.id "
                + "WHERE e.quantidade <= e.estoque_minimo AND p.ativo = true ORDER BY e.quantidade ASC";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new String[]{rs.getString("nome"),
                    rs.getString("quantidade"), rs.getString("estoque_minimo")});
        }
        return lista;
    }
}
