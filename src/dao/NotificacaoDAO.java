package dao;
import util.*;
import java.sql.*;
import java.util.*;

/** Avisos locais persistidos com a operação; leitura individual e idempotente. */
public final class NotificacaoDAO {
    public record Aviso(long id,Timestamp data,String tipo,String mensagem,boolean lida) {}
    public List<Aviso> listar(boolean somenteNaoLidas)throws SQLException{
        try(Connection c=ConexaoBanco.getConexao()){
            boolean equipe=autorizar(c);String ator=equipe?"id_usuario":"id_cliente";int id=equipe?Sessao.getIdUsuario():Sessao.getIdCliente();
            String sql="SELECT n.*,l.id IS NOT NULL AS lida FROM notificacoes n LEFT JOIN leituras_notificacoes l ON l.id_notificacao=n.id AND l."+ator+"=? WHERE "+(equipe?"n.destino='equipe'":"n.destino='cliente' AND n.id_cliente=?")+(somenteNaoLidas?" AND l.id IS NULL":"")+" ORDER BY n.id DESC LIMIT 500";
            List<Aviso> lista=new ArrayList<>();try(PreparedStatement p=c.prepareStatement(sql)){
                p.setInt(1,id);if(!equipe)p.setInt(2,id);try(ResultSet r=p.executeQuery()){while(r.next())lista.add(new Aviso(r.getLong("id"),r.getTimestamp("criada_em"),r.getString("tipo"),r.getString("mensagem"),r.getBoolean("lida")));}
            }return lista;
        }
    }
    public void marcarLida(long aviso)throws SQLException{
        Transacao.executar(c->{boolean equipe=autorizar(c);int id=equipe?Sessao.getIdUsuario():Sessao.getIdCliente();
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM notificacoes WHERE id=? AND "+(equipe?"destino='equipe'":"destino='cliente' AND id_cliente=?"))){p.setLong(1,aviso);if(!equipe)p.setInt(2,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Notificação não encontrada.");}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO leituras_notificacoes(id_notificacao,"+(equipe?"id_usuario":"id_cliente")+") VALUES(?,?) ON CONFLICT DO NOTHING")){p.setLong(1,aviso);p.setInt(2,id);p.executeUpdate();}return null;
        });
    }
    private static boolean autorizar(Connection c)throws SQLException{boolean equipe=Sessao.getIdUsuario()>0;if(equipe)Acesso.equipe(c,false);else Acesso.cliente(c,Sessao.getIdCliente());return equipe;}
    static void pedido(Connection c,int pedido,long historico,String anterior,String status,String motivo)throws SQLException{
        int cliente;try(PreparedStatement p=c.prepareStatement("SELECT id_cliente FROM pedidos WHERE id=?")){p.setInt(1,pedido);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Pedido ausente.");cliente=r.getInt(1);}}
        String tipo=Objects.equals(anterior,status)?"pagamento":"pedido";
        String mensagem="Pedido #"+pedido+" — "+StatusPedido.nome(status)+". "+motivo;
        inserir(c,"historico:"+historico+":cliente","cliente",cliente,tipo,mensagem,pedido,null);
        inserir(c,"historico:"+historico+":equipe","equipe",null,tipo,mensagem,pedido,null);
    }
    static void estoque(Connection c,int produto)throws SQLException{
        boolean baixo,anterior;long episodio;String nome;int saldo,minimo;
        try(PreparedStatement p=c.prepareStatement("SELECT e.*,pr.nome,pr.ativo FROM estoque e JOIN produtos pr ON pr.id=e.id_produto WHERE e.id_produto=? FOR UPDATE OF e")){
            p.setInt(1,produto);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Estoque ausente.");saldo=r.getInt("quantidade");minimo=r.getInt("estoque_minimo");baixo=r.getBoolean("ativo") && saldo<=minimo;anterior=r.getBoolean("alerta_ativo");episodio=r.getLong("episodio_alerta");nome=r.getString("nome");}
        }
        if(baixo && !anterior){episodio++;inserir(c,"estoque:"+produto+":"+episodio,"equipe",null,"estoque","Estoque baixo: "+nome+". Saldo "+saldo+"; mínimo "+minimo+".",null,produto);}
        try(PreparedStatement p=c.prepareStatement("UPDATE estoque SET alerta_ativo=?,episodio_alerta=? WHERE id_produto=?")){p.setBoolean(1,baixo);p.setLong(2,episodio);p.setInt(3,produto);p.executeUpdate();}
    }
    private static void inserir(Connection c,String chave,String destino,Integer cliente,String tipo,String mensagem,Integer pedido,Integer produto)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO notificacoes(chave,destino,id_cliente,tipo,mensagem,id_pedido,id_produto) VALUES(?,?,?,?,?,?,?) ON CONFLICT(chave) DO NOTHING")){
            p.setString(1,chave);p.setString(2,destino);if(cliente==null)p.setNull(3,Types.INTEGER);else p.setInt(3,cliente);p.setString(4,tipo);p.setString(5,mensagem);if(pedido==null)p.setNull(6,Types.INTEGER);else p.setInt(6,pedido);if(produto==null)p.setNull(7,Types.INTEGER);else p.setInt(7,produto);p.executeUpdate();
        }
    }
}
