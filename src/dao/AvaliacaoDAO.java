package dao;
import util.*;
import java.sql.*;
import java.util.*;

public final class AvaliacaoDAO {
    public record ProdutoEntregue(int id,String nome) {public String toString(){return nome;}}
    public record Avaliacao(int nota,String comentario,Timestamp data) {}
    public List<ProdutoEntregue> produtosEntregues(int pedido)throws SQLException{
        List<ProdutoEntregue> lista=new ArrayList<>();try(Connection c=ConexaoBanco.getConexao()){
            Acesso.cliente(c,Sessao.getIdCliente());
            try(PreparedStatement p=c.prepareStatement("SELECT DISTINCT pr.id,pr.nome FROM pedidos pd JOIN itens_pedido i ON i.id_pedido=pd.id JOIN produtos pr ON pr.id=i.id_produto JOIN pagamentos pg ON pg.id_pedido=pd.id WHERE pd.id=? AND pd.id_cliente=? AND pd.status='entregue' AND pg.status='confirmado' ORDER BY pr.nome")){
                p.setInt(1,pedido);p.setInt(2,Sessao.getIdCliente());try(ResultSet r=p.executeQuery()){while(r.next())lista.add(new ProdutoEntregue(r.getInt(1),r.getString(2)));}
            }
        }return lista;
    }
    public Avaliacao minha(int produto)throws SQLException{
        try(Connection c=ConexaoBanco.getConexao()){Acesso.cliente(c,Sessao.getIdCliente());
            try(PreparedStatement p=c.prepareStatement("SELECT nota,comentario,atualizada_em FROM avaliacoes WHERE id_cliente=? AND id_produto=?")){
                p.setInt(1,Sessao.getIdCliente());p.setInt(2,produto);try(ResultSet r=p.executeQuery()){return r.next()?new Avaliacao(r.getInt(1),r.getString(2),r.getTimestamp(3)):null;}
            }
        }
    }
    public void salvar(int pedido,int produto,int nota,String comentario)throws SQLException{
        if(nota<1 || nota>5)throw new IllegalArgumentException("Nota deve ser de 1 a 5.");
        String texto=Validacao.texto(comentario,"Comentário",0,1000);
        int cliente=Sessao.getIdCliente();
        Transacao.executar(c->{Acesso.cliente(c,cliente);
            try(PreparedStatement p=c.prepareStatement("SELECT pd.id FROM pedidos pd JOIN pagamentos pg ON pg.id_pedido=pd.id WHERE pd.id=? AND pd.id_cliente=? AND pd.status='entregue' AND pg.status='confirmado' AND EXISTS(SELECT 1 FROM itens_pedido i WHERE i.id_pedido=pd.id AND i.id_produto=?) FOR SHARE OF pd")){
                p.setInt(1,pedido);p.setInt(2,cliente);p.setInt(3,produto);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Avalie somente produtos dos seus pedidos entregues e pagos.");}
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO avaliacoes(id_cliente,id_produto,id_pedido,nota,comentario) VALUES(?,?,?,?,?) ON CONFLICT(id_cliente,id_produto) DO UPDATE SET nota=EXCLUDED.nota,comentario=EXCLUDED.comentario,atualizada_em=CURRENT_TIMESTAMP")){
                p.setInt(1,cliente);p.setInt(2,produto);p.setInt(3,pedido);p.setInt(4,nota);p.setString(5,texto.isEmpty()?null:texto);if(p.executeUpdate()!=1)throw new SQLException("Avaliação não gravada.");
            }return null;
        });
    }
    /** Exibe só nota/comentário/data; não publica nome, CPF ou e-mail do comprador. */
    public List<Avaliacao> listarProduto(int produto)throws SQLException{
        List<Avaliacao> lista=new ArrayList<>();try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT nota,comentario,atualizada_em FROM avaliacoes WHERE id_produto=? ORDER BY atualizada_em DESC LIMIT 50")){
            p.setInt(1,produto);try(ResultSet r=p.executeQuery()){while(r.next())lista.add(new Avaliacao(r.getInt(1),r.getString(2),r.getTimestamp(3)));}
        }return lista;
    }
}
