package dao;
import util.*;
import java.sql.*;
import java.util.*;

public class EstoqueDAO {
    public int getQuantidade(int id)throws SQLException{
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT quantidade FROM estoque WHERE id_produto=?")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}
        }
    }
    public int getMinimo(int id)throws SQLException{
        Acesso.equipe();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT estoque_minimo FROM estoque WHERE id_produto=?")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Estoque inexistente.");return r.getInt(1);}
        }
    }
    public void definirMinimo(int id,int minimo)throws SQLException{
        if(minimo<0)throw new IllegalArgumentException("Mínimo não pode ser negativo.");
        Transacao.executar(c->{Acesso.equipe(c,true);
            try(PreparedStatement p=c.prepareStatement("UPDATE estoque SET estoque_minimo=? WHERE id_produto=?")){
                p.setInt(1,minimo);p.setInt(2,id);if(p.executeUpdate()!=1)throw new SQLException("Estoque inexistente.");
            }return null;
        });
    }
    public void atualizar(int id,int quantidade)throws SQLException{
        if(quantidade<0)throw new IllegalArgumentException("Estoque não pode ser negativo.");
        Transacao.executar(c->{Acesso.equipe(c,true);int anterior;
            try(PreparedStatement p=c.prepareStatement("SELECT quantidade FROM estoque WHERE id_produto=? FOR UPDATE")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Produto sem registro de estoque.");anterior=r.getInt(1);}
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE estoque SET quantidade=? WHERE id_produto=?")){p.setInt(1,quantidade);p.setInt(2,id);p.executeUpdate();}
            registrar(c,id,"ajuste",quantidade-anterior,quantidade,"Ajuste de saldo");return null;
        });
    }
    public void entrada(int id,int qtd)throws SQLException{entrada(id,qtd,"Reposição manual");}
    public void entrada(int id,int qtd,String motivo)throws SQLException{manual(id,qtd,true,motivo);}
    public void saida(int id,int qtd)throws SQLException{saida(id,qtd,"Saída manual");}
    public void saida(int id,int qtd,String motivo)throws SQLException{manual(id,qtd,false,motivo);}
    private void manual(int id,int qtd,boolean entrada,String motivo)throws SQLException{
        validar(qtd);String m=Validacao.texto(motivo,"Motivo",3,255);
        Transacao.executar(c->{Acesso.equipe(c,true);mover(c,id,qtd,entrada,entrada?"entrada":"saida",m);return null;});
    }
    /** Usada pelo pedido já autorizado e dentro de sua transação. */
    public void saida(Connection c,int id,int qtd)throws SQLException{
        saida(c,id,qtd,null);
    }
    void saida(Connection c,int id,int qtd,Integer pedido)throws SQLException{
        if(c.getAutoCommit())throw new IllegalArgumentException("Saída de venda exige transação.");
        // Revalida a identidade também no ponto público de movimentação compartilhada.
        if(Sessao.getIdUsuario()>0) Acesso.equipe(c,false);else Acesso.cliente(c,Sessao.getIdCliente());
        mover(c,id,qtd,false,"venda","Baixa por venda",pedido);
    }
    /** A gestão já bloqueou e autorizou o pedido; devolução usa a mesma transação. */
    void devolver(Connection c,int pedido,int produto,int qtd,String motivo)throws SQLException{
        if(c.getAutoCommit())throw new IllegalArgumentException("Devolução exige transação.");
        validar(qtd);
        // Mesma ordem de locks usada pelas compras: produto e depois saldo, por ID.
        try(PreparedStatement p=c.prepareStatement("SELECT e.quantidade FROM produtos pr JOIN estoque e ON e.id_produto=pr.id WHERE pr.id=? FOR UPDATE OF pr,e")){
            p.setInt(1,produto);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Produto sem estoque para devolução.");if((long)r.getInt(1)+qtd>Integer.MAX_VALUE)throw new IllegalArgumentException("O saldo excederia o limite do estoque.");}
        }
        mover(c,produto,qtd,true,"devolucao",motivo.substring(0,Math.min(motivo.length(),255)),pedido);
    }
    private void mover(Connection c,int id,int qtd,boolean entrada,String tipo,String motivo)throws SQLException{
        mover(c,id,qtd,entrada,tipo,motivo,null);
    }
    private void mover(Connection c,int id,int qtd,boolean entrada,String tipo,String motivo,Integer pedido)throws SQLException{
        validar(qtd);String sql=entrada?"UPDATE estoque SET quantidade=quantidade+? WHERE id_produto=? RETURNING quantidade":
                "UPDATE estoque SET quantidade=quantidade-? WHERE id_produto=? AND quantidade>=? RETURNING quantidade";
        int saldo;
        try(PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,qtd);p.setInt(2,id);if(!entrada)p.setInt(3,qtd);
            try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Estoque insuficiente ou produto inexistente: "+id);saldo=r.getInt(1);}
        }
        registrar(c,id,tipo,entrada?qtd:-qtd,saldo,motivo,pedido);
    }
    static void registrar(Connection c,int id,String tipo,int qtd,int saldo,String motivo)throws SQLException{
        registrar(c,id,tipo,qtd,saldo,motivo,null);
    }
    private static void registrar(Connection c,int id,String tipo,int qtd,int saldo,String motivo,Integer pedido)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO movimentos_estoque(id_produto,tipo,quantidade,saldo,motivo,id_usuario,id_cliente,id_pedido) VALUES(?,?,?,?,?,?,?,?)")){
            p.setInt(1,id);p.setString(2,tipo);p.setInt(3,qtd);p.setInt(4,saldo);p.setString(5,motivo);
            if(Sessao.getIdUsuario()>0)p.setInt(6,Sessao.getIdUsuario());else p.setNull(6,Types.INTEGER);
            if(Sessao.getIdCliente()>0)p.setInt(7,Sessao.getIdCliente());else p.setNull(7,Types.INTEGER);
            if(pedido!=null)p.setInt(8,pedido);else p.setNull(8,Types.INTEGER);
            if(p.executeUpdate()!=1)throw new SQLException("Movimento não registrado.");
        }
    }
    public List<String[]> historico(int id)throws SQLException{
        Acesso.equipe();List<String[]> lista=new ArrayList<>();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT m.*,COALESCE(u.nome,cl.nome,'Instalação') AS responsavel FROM movimentos_estoque m LEFT JOIN usuarios u ON u.id=m.id_usuario LEFT JOIN clientes cl ON cl.id=m.id_cliente WHERE m.id_produto=? ORDER BY m.id DESC")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())lista.add(new String[]{r.getTimestamp("data_movimento").toString(),r.getString("tipo"),r.getString("quantidade"),r.getString("saldo"),r.getString("motivo"),r.getString("responsavel")});}
        }return lista;
    }
    public List<String[]> listarEstoqueBaixo()throws SQLException{
        Acesso.equipe();List<String[]> lista=new ArrayList<>();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT p.nome,e.quantidade,e.estoque_minimo FROM estoque e JOIN produtos p ON p.id=e.id_produto WHERE p.ativo AND e.quantidade<=e.estoque_minimo ORDER BY e.quantidade");ResultSet r=p.executeQuery()){
            while(r.next())lista.add(new String[]{r.getString(1),r.getString(2),r.getString(3)});
        }return lista;
    }
    private static void validar(int qtd){if(qtd<=0)throw new IllegalArgumentException("A quantidade deve ser maior que zero.");}
}
