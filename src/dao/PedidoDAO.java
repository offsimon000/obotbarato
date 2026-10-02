package dao;

import model.ItemPedido;
import model.Pedido;
import util.ConexaoBanco;
import util.Sessao;
import util.Transacao;
import java.time.LocalDate;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAO {

    public void salvar(Pedido pedido) throws SQLException {
        if (pedido.getId() != 0) throw new IllegalArgumentException("Este pedido já foi gravado.");
        if (pedido.getIdCliente() <= 0 || pedido.getItens() == null || pedido.getItens().isEmpty())
            throw new IllegalArgumentException("Informe o cliente e pelo menos um item.");
        for (ItemPedido item : pedido.getItens()) {
            if (item == null || item.getIdProduto() <= 0 || item.getQuantidade() <= 0)
                throw new IllegalArgumentException("Item de pedido inválido.");
        }
        pedido.recalcularTotal();
        int id = Transacao.executar(con -> {
            util.Acesso.equipe(con,false);
            util.Acesso.venda(con, pedido.getIdCliente());
            try(Statement s=con.createStatement()){s.execute("SELECT pg_advisory_xact_lock_shared(726282)");}
            List<ItemPedido> ordenados=pedido.getItens().stream().sorted(java.util.Comparator.comparingInt(ItemPedido::getIdProduto)).toList();
            for(ItemPedido item:ordenados){
                try(PreparedStatement p=con.prepareStatement("SELECT p.preco,e.quantidade FROM produtos p JOIN categorias c ON c.id=p.id_categoria JOIN estoque e ON e.id_produto=p.id WHERE p.id=? AND p.ativo AND c.ativo FOR UPDATE OF p,e")){
                    p.setInt(1,item.getIdProduto());try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new IllegalArgumentException("Produto indisponível.");
                        if(r.getBigDecimal(1).compareTo(item.getPrecoUnitarioDecimal())!=0)throw new IllegalArgumentException("Preço alterado. Refaça os itens da venda.");
                    }
                }
            }
            int idNovo;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pedidos (id_cliente, status, total, registrado_por,subtotal) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, pedido.getIdCliente());
                ps.setString(2, pedido.getStatus());
                ps.setBigDecimal(3, pedido.getTotalDecimal());
                if (Sessao.getIdUsuario() > 0) ps.setInt(4, Sessao.getIdUsuario());
                else ps.setNull(4, Types.INTEGER);
                ps.setBigDecimal(5,pedido.getTotalDecimal());
                if (ps.executeUpdate() != 1) throw new SQLException("Não foi possível gravar o pedido.");
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Pedido sem identificador gerado.");
                    idNovo = rs.getInt(1);
                }
            }
            EstoqueDAO estoqueDAO = new EstoqueDAO();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO itens_pedido (id_pedido, id_produto, quantidade, preco_unitario) VALUES (?,?,?,?)")) {
                for (ItemPedido item : ordenados) {
                    ps.setInt(1, idNovo);
                    ps.setInt(2, item.getIdProduto());
                    ps.setInt(3, item.getQuantidade());
                    ps.setBigDecimal(4, item.getPrecoUnitarioDecimal());
                    if (ps.executeUpdate() != 1) throw new SQLException("Não foi possível gravar o item.");
                    estoqueDAO.saida(con, item.getIdProduto(), item.getQuantidade());
                }
            }
            try(PreparedStatement pg=con.prepareStatement("INSERT INTO pagamentos(id_pedido,metodo,status,confirmado_em) VALUES(?,'dinheiro',?,CASE WHEN ?='confirmado' THEN CURRENT_TIMESTAMP ELSE NULL END)")){
                String pagamento="finalizado".equals(pedido.getStatus())?"confirmado":"cancelado".equals(pedido.getStatus())?"cancelado":"pendente";
                pg.setInt(1,idNovo);pg.setString(2,pagamento);pg.setString(3,pagamento);pg.executeUpdate();
            }
            return idNovo;
        });
        // O objeto só recebe o ID depois da confirmação de toda a transação.
        pedido.setId(id);
    }

    public List<Pedido> listarPorCliente(int idCliente) throws SQLException {
        if (Sessao.getIdUsuario()>0) util.Acesso.equipe(); else util.Acesso.cliente(idCliente);
        if (idCliente <= 0) throw new IllegalArgumentException("Cliente inválido.");
        return listar(idCliente);
    }

    public List<Pedido> listarTodos() throws SQLException { util.Acesso.equipe(); return listar(null); }

    private List<Pedido> listar(Integer idCliente) throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        String sql = "SELECT p.*, c.nome AS cliente,pg.metodo AS metodo_pagamento,pg.status AS status_pagamento FROM pedidos p "
                + "JOIN clientes c ON p.id_cliente = c.id JOIN pagamentos pg ON pg.id_pedido=p.id "
                + (idCliente != null ? "WHERE p.id_cliente = ? " : "")
                + "ORDER BY p.data_pedido DESC, p.id DESC";
        try (Connection con = ConexaoBanco.getConexao(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (idCliente != null) ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pedido p = new Pedido();
                    p.setId(rs.getInt("id"));
                    p.setIdCliente(rs.getInt("id_cliente"));
                    p.setNomeCliente(rs.getString("cliente"));
                    p.setDataPedido(rs.getTimestamp("data_pedido"));
                    p.setStatus(rs.getString("status"));
                    p.setTotal(rs.getBigDecimal("total"));
                    p.setMetodoPagamento(rs.getString("metodo_pagamento"));p.setStatusPagamento(rs.getString("status_pagamento"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    // Relatório: total de vendas por período
    public List<String[]> relatorioVendasPorPeriodo(String dataInicio, String dataFim) throws SQLException {
        util.Acesso.equipe();
        LocalDate inicio = LocalDate.parse(dataInicio);
        LocalDate fim = LocalDate.parse(dataFim);
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("Data final anterior à inicial.");
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT DATE(data_pedido) AS dia, COUNT(*) AS qtd_pedidos, SUM(total) AS total_dia "
                   + "FROM pedidos WHERE status = 'finalizado' "
                   + "AND DATE(data_pedido) BETWEEN ? AND ? "
                   + "GROUP BY DATE(data_pedido) ORDER BY dia";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new String[]{
                        rs.getString("dia"),
                        rs.getString("qtd_pedidos"),
                        String.format("%.2f", rs.getBigDecimal("total_dia"))
                    });
                }
            }
        }
        return lista;
    }

    // Relatório: produtos mais vendidos
    public List<String[]> relatorioProdutosMaisVendidos() throws SQLException {
        util.Acesso.equipe();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT p.nome, SUM(i.quantidade) AS total_vendido, SUM(i.quantidade * i.preco_unitario) AS receita "
                   + "FROM itens_pedido i "
                   + "JOIN produtos p ON i.id_produto = p.id "
                   + "JOIN pedidos pd ON i.id_pedido = pd.id "
                   + "WHERE pd.status = 'finalizado' "
                   + "GROUP BY p.id, p.nome ORDER BY total_vendido DESC LIMIT 10";
        try (Connection con = ConexaoBanco.getConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{
                    rs.getString("nome"),
                    rs.getString("total_vendido"),
                    String.format("R$ %.2f", rs.getBigDecimal("receita"))
                });
            }
        }
        return lista;
    }
}
