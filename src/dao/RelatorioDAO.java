package dao;

import util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Um único snapshot de leitura para indicadores, tabelas e exportação. */
public final class RelatorioDAO {
    public record VendaDia(LocalDate data,long pedidos,BigDecimal bruto,BigDecimal cupom,BigDecimal pagamento,BigDecimal liquido) {}
    public record Ranking(int id,String nome,long quantidade,BigDecimal bruto) {}
    public record PedidoResumo(int id,Timestamp data,String cliente,String origem,String status,String pagamento,BigDecimal bruto,BigDecimal cupom,BigDecimal descontoPagamento,BigDecimal liquido) {}
    public record EstoqueLinha(int id,String nome,int quantidade,int minimo) {}
    public record Resultado(LocalDate inicio,LocalDate fim,List<VendaDia> vendas,List<Ranking> ranking,List<PedidoResumo> pedidos,List<EstoqueLinha> estoque) {
        public BigDecimal faturamento(){return vendas.stream().map(VendaDia::liquido).reduce(new BigDecimal("0.00"),BigDecimal::add);}
        public long confirmados(){return vendas.stream().mapToLong(VendaDia::pedidos).sum();}
        public BigDecimal ticket(){return confirmados()==0?new BigDecimal("0.00"):faturamento().divide(BigDecimal.valueOf(confirmados()),2,RoundingMode.HALF_UP);}
        public long pendentes(){return pedidos.stream().filter(p->!p.status().equals("cancelado") && p.pagamento().equals("pendente")).count();}
        public long cancelados(){return pedidos.stream().filter(p->p.status().equals("cancelado")).count();}
        public long quantidadeStatus(String status){return pedidos.stream().filter(p->p.status().equals(status)).count();}
    }
    private static final String PERIODO=" p.data_pedido>=? AND p.data_pedido<? ";
    private static final String PAGOS=" p.status<>'cancelado' AND pg.status='confirmado' ";

    public Resultado consultar(LocalDate inicio,LocalDate fim)throws SQLException {
        if(inicio==null || fim==null || fim.isBefore(inicio))throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        if(inicio.getYear()<1 || fim.getYear()>9999)throw new IllegalArgumentException("Informe anos entre 0001 e 9999.");
        try(Connection c=ConexaoBanco.getConexao()) {
            c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
            Acesso.equipe(c,false);
            List<VendaDia> vendas=new ArrayList<>();
            try(PreparedStatement p=periodo(c,"SELECT DATE(p.data_pedido),COUNT(*),SUM(p.subtotal),SUM(p.desconto_cupom),SUM(p.desconto_pagamento),SUM(p.total) FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE "+PERIODO+" AND "+PAGOS+" GROUP BY DATE(p.data_pedido) ORDER BY 1",inicio,fim);ResultSet r=p.executeQuery()){
                while(r.next())vendas.add(new VendaDia(r.getDate(1).toLocalDate(),r.getLong(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getBigDecimal(5),r.getBigDecimal(6)));
            }
            List<Ranking> ranking=new ArrayList<>();
            try(PreparedStatement p=periodo(c,"SELECT pr.id,pr.nome,SUM(i.quantidade),SUM(i.quantidade*i.preco_unitario) FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id JOIN itens_pedido i ON i.id_pedido=p.id JOIN produtos pr ON pr.id=i.id_produto WHERE "+PERIODO+" AND "+PAGOS+" GROUP BY pr.id,pr.nome ORDER BY 3 DESC,4 DESC,pr.nome,pr.id LIMIT 10",inicio,fim);ResultSet r=p.executeQuery()){
                while(r.next())ranking.add(new Ranking(r.getInt(1),r.getString(2),r.getLong(3),r.getBigDecimal(4)));
            }
            List<PedidoResumo> pedidos=new ArrayList<>();
            try(PreparedStatement p=periodo(c,"SELECT p.id,p.data_pedido,cl.nome,p.origem,p.status,COALESCE(pg.status,'sem registro'),p.subtotal,p.desconto_cupom,p.desconto_pagamento,p.total FROM pedidos p JOIN clientes cl ON cl.id=p.id_cliente LEFT JOIN pagamentos pg ON pg.id_pedido=p.id WHERE "+PERIODO+" ORDER BY p.data_pedido DESC,p.id DESC",inicio,fim);ResultSet r=p.executeQuery()){
                while(r.next())pedidos.add(new PedidoResumo(r.getInt(1),r.getTimestamp(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getBigDecimal(7),r.getBigDecimal(8),r.getBigDecimal(9),r.getBigDecimal(10)));
            }
            List<EstoqueLinha> estoque=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement("SELECT pr.id,pr.nome,e.quantidade,e.estoque_minimo FROM produtos pr JOIN estoque e ON e.id_produto=pr.id WHERE pr.ativo AND e.quantidade<=e.estoque_minimo ORDER BY e.quantidade,pr.nome,pr.id");ResultSet r=p.executeQuery()){
                while(r.next())estoque.add(new EstoqueLinha(r.getInt(1),r.getString(2),r.getInt(3),r.getInt(4)));
            }
            c.commit();return new Resultado(inicio,fim,List.copyOf(vendas),List.copyOf(ranking),List.copyOf(pedidos),List.copyOf(estoque));
        }
    }
    private PreparedStatement periodo(Connection c,String sql,LocalDate inicio,LocalDate fim)throws SQLException {
        PreparedStatement p=c.prepareStatement(sql);
        p.setTimestamp(1,Timestamp.valueOf(inicio.atStartOfDay()));p.setTimestamp(2,Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));return p;
    }
}
