package dao;

import model.ItemPedido;
import util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Confirma compra e pagamento fictício sem confiar em preço ou estado do catálogo na tela. */
public final class CompraDAO {
    public record Resultado(int id,BigDecimal subtotal,BigDecimal desconto,BigDecimal total,String metodo,String pagamento) {}

    public Resultado confirmar(int idCliente,List<ItemPedido> itens,String metodo,String nome,String telefone,String endereco,UUID chave)throws SQLException {
        if(chave==null)throw new IllegalArgumentException("Tentativa de compra inválida.");
        if(metodo==null || !Set.of("credito","debito","pix","boleto").contains(metodo))throw new IllegalArgumentException("Selecione o pagamento simulado.");
        String destinatario=Validacao.texto(nome,"Destinatário",3,120);
        String tel=Validacao.telefone(telefone);if(tel==null)throw new IllegalArgumentException("Informe o telefone de entrega.");
        String end=Validacao.texto(endereco,"Endereço de entrega",5,255);
        if(itens==null || itens.isEmpty())throw new IllegalArgumentException("Carrinho vazio.");
        TreeMap<Integer,ItemPedido> copia=new TreeMap<>();
        for(ItemPedido i:itens){
            if(i==null || i.getIdProduto()<=0 || i.getQuantidade()<=0)throw new IllegalArgumentException("Item inválido.");
            if(copia.containsKey(i.getIdProduto()))throw new IllegalArgumentException("Produto repetido no carrinho.");
            ItemPedido c=new ItemPedido();c.setIdProduto(i.getIdProduto());c.setQuantidade(i.getQuantidade());c.setPrecoUnitario(i.getPrecoUnitarioDecimal());copia.put(c.getIdProduto(),c);
        }
        return Transacao.executar(con -> {
            Acesso.cliente(con,idCliente);
            try(PreparedStatement p=con.prepareStatement("SELECT pg_advisory_xact_lock(726285,?)")){p.setInt(1,chave.hashCode());p.execute();}
            try(PreparedStatement p=con.prepareStatement("SELECT p.*,pg.metodo,pg.status AS pagamento FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.chave_compra=? AND p.id_cliente=?")){
                p.setObject(1,chave);p.setInt(2,idCliente);try(ResultSet r=p.executeQuery()){if(r.next())return resultado(r);}
            }
            // Categorias/produtos podem ser mantidos enquanto outras compras leem; mudança de categoria espera.
            try(Statement s=con.createStatement()){s.execute("SELECT pg_advisory_xact_lock_shared(726282)");}
            BigDecimal subtotal=new BigDecimal("0.00");
            for(ItemPedido item:copia.values()){
                try(PreparedStatement p=con.prepareStatement("SELECT p.nome,p.preco,e.quantidade FROM produtos p JOIN categorias c ON c.id=p.id_categoria JOIN estoque e ON e.id_produto=p.id WHERE p.id=? AND p.ativo AND c.ativo FOR UPDATE OF p,e")){
                    p.setInt(1,item.getIdProduto());try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new IllegalArgumentException("Um produto foi desativado ou removido do catálogo. Atualize o carrinho.");
                        item.setNomeProduto(r.getString(1));
                        if(item.getPrecoUnitarioDecimal().compareTo(r.getBigDecimal(2))!=0)throw new IllegalArgumentException("O preço de "+item.getNomeProduto()+" mudou. Atualize sua quantidade no carrinho e revise o novo total.");
                        if(item.getQuantidade()>r.getInt(3))throw new IllegalArgumentException("Estoque insuficiente para "+item.getNomeProduto()+". Atualize o carrinho.");
                    }
                }subtotal=subtotal.add(item.getSubtotalDecimal());
            }
            BigDecimal desconto=PagamentoSimulado.desconto(subtotal,metodo),total=subtotal.subtract(desconto);
            String pagamento=PagamentoSimulado.status(metodo);int id;
            try(PreparedStatement p=con.prepareStatement("INSERT INTO pedidos(id_cliente,status,total,subtotal,desconto,origem,chave_compra,destinatario,telefone_entrega,endereco_entrega) VALUES(?,?,?,?,?,'loja',?,?,?,?) RETURNING id")){
                p.setInt(1,idCliente);p.setString(2,"aberto");p.setBigDecimal(3,total);p.setBigDecimal(4,subtotal);p.setBigDecimal(5,desconto);p.setObject(6,chave);p.setString(7,destinatario);p.setString(8,tel);p.setString(9,end);
                try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Pedido não gravado.");id=r.getInt(1);}
            }
            for(ItemPedido item:copia.values()){
                try(PreparedStatement p=con.prepareStatement("INSERT INTO itens_pedido(id_pedido,id_produto,quantidade,preco_unitario) VALUES(?,?,?,?)")){
                    p.setInt(1,id);p.setInt(2,item.getIdProduto());p.setInt(3,item.getQuantidade());p.setBigDecimal(4,item.getPrecoUnitarioDecimal());p.executeUpdate();
                }new EstoqueDAO().saida(con,item.getIdProduto(),item.getQuantidade(),id);
            }
            try(PreparedStatement p=con.prepareStatement("INSERT INTO pagamentos(id_pedido,metodo,status,confirmado_em) VALUES(?,?,?,CASE WHEN ?='confirmado' THEN CURRENT_TIMESTAMP ELSE NULL END)")){
                p.setInt(1,id);p.setString(2,metodo);p.setString(3,pagamento);p.setString(4,pagamento);p.executeUpdate();
            }
            GestaoPedidoDAO.registrar(con,id,null,"aberto","Compra na loja; pagamento simulado "+pagamento);
            return new Resultado(id,subtotal,desconto,total,metodo,pagamento);
        });
    }
    public void confirmarPagamento(int id)throws SQLException {
        Transacao.executar(con->{
            Acesso.equipe(con,true);
            try(PreparedStatement p=con.prepareStatement("SELECT p.status,pg.status FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.id=? FOR UPDATE OF p,pg")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new IllegalArgumentException("Pedido não encontrado.");
                    if(r.getString(1).equals("cancelado") || r.getString(2).equals("cancelado"))throw new IllegalArgumentException("Pedido cancelado não pode ter pagamento confirmado.");
                    if(r.getString(2).equals("confirmado"))return null;
                    if(!r.getString(1).equals("aberto") || !r.getString(2).equals("pendente"))throw new IllegalArgumentException("Este pagamento não pode ser confirmado.");
                }
            }
            try(PreparedStatement p=con.prepareStatement("UPDATE pagamentos SET status='confirmado',confirmado_em=CURRENT_TIMESTAMP,confirmado_por=? WHERE id_pedido=?")){p.setInt(1,Sessao.getIdUsuario());p.setInt(2,id);p.executeUpdate();}
            GestaoPedidoDAO.registrar(con,id,"aberto","aberto","Pagamento simulado confirmado pelo administrador");return null;
        });
    }
    public List<String[]> listarPagamentos()throws SQLException {
        Acesso.equipe();List<String[]> lista=new ArrayList<>();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT p.id,c.nome,p.total,pg.metodo,pg.status,p.origem FROM pedidos p JOIN clientes c ON c.id=p.id_cliente JOIN pagamentos pg ON pg.id_pedido=p.id ORDER BY p.id DESC");ResultSet r=p.executeQuery()){
            while(r.next())lista.add(new String[]{r.getString(1),r.getString(2),PagamentoSimulado.moeda(r.getBigDecimal(3)),PagamentoSimulado.NOMES.get(r.getString(4)),r.getString(5),r.getString(6)});
        }return lista;
    }
    public String detalhes(int id)throws SQLException {
        try(Connection c=ConexaoBanco.getConexao()){
            boolean equipe=Sessao.getIdUsuario()>0;
            if(equipe)Acesso.equipe(c,false);else Acesso.cliente(c,Sessao.getIdCliente());
            StringBuilder texto=new StringBuilder();
            try(PreparedStatement p=c.prepareStatement("SELECT p.*,pg.metodo,pg.status AS pagamento,c.nome FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id JOIN clientes c ON c.id=p.id_cliente WHERE p.id=? "+(equipe?"":"AND p.id_cliente=?"))){
                p.setInt(1,id);if(!equipe)p.setInt(2,Sessao.getIdCliente());
                try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new IllegalArgumentException("Pedido não encontrado.");
                    texto.append("Pedido #").append(id).append(" — ").append(r.getTimestamp("data_pedido")).append("\nCliente: ").append(r.getString("nome")).append("\nOrigem: ").append(r.getString("origem")).append("\nStatus do pedido: ").append(StatusPedido.nome(r.getString("status")));
                    texto.append("\nPagamento SIMULADO: ").append(PagamentoSimulado.NOMES.get(r.getString("metodo"))).append(" / ").append(r.getString("pagamento"));
                    texto.append("\nSubtotal: ").append(PagamentoSimulado.moeda(r.getBigDecimal("subtotal"))).append("\nDesconto: ").append(PagamentoSimulado.moeda(r.getBigDecimal("desconto"))).append("\nTotal: ").append(PagamentoSimulado.moeda(r.getBigDecimal("total")));
                    texto.append("\nEntrega simulada: ").append(Objects.toString(r.getString("destinatario"),"Retirada no balcão")).append("\n").append(Objects.toString(r.getString("endereco_entrega"),"")).append("\n").append(Objects.toString(r.getString("telefone_entrega"),"")).append("\n\nItens:\n");
                }
            }
            try(PreparedStatement p=c.prepareStatement("SELECT pr.nome,i.quantidade,i.preco_unitario FROM itens_pedido i JOIN produtos pr ON pr.id=i.id_produto WHERE i.id_pedido=? ORDER BY i.id")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())texto.append(r.getInt(2)).append(" × ").append(r.getString(1)).append(" / ").append(PagamentoSimulado.moeda(r.getBigDecimal(3))).append("\n");}
            }texto.append(GestaoPedidoDAO.historico(c,id));return texto.toString();
        }
    }
    private static Resultado resultado(ResultSet r)throws SQLException{return new Resultado(r.getInt("id"),r.getBigDecimal("subtotal"),r.getBigDecimal("desconto"),r.getBigDecimal("total"),r.getString("metodo"),r.getString("pagamento"));}
}
