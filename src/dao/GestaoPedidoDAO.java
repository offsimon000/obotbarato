package dao;

import util.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Consulta, acompanhamento e cancelamento atômico de pedidos. */
public final class GestaoPedidoDAO {
    public record Filtro(LocalDate inicio,LocalDate fim,String cliente,String status,String origem) {}
    public record Resumo(int id,Timestamp data,String cliente,String origem,String status,String pagamento,String total) {}
    private record Estado(String status,String pagamento,int cliente) {}

    public List<Resumo> listar(Filtro filtro)throws SQLException{
        Filtro f=filtro==null?new Filtro(null,null,null,null,null):filtro;
        if(f.inicio()!=null && f.fim()!=null && f.fim().isBefore(f.inicio()))throw new IllegalArgumentException("Data final anterior à inicial.");
        if(f.status()!=null && !StatusPedido.TODOS.contains(f.status()))throw new IllegalArgumentException("Status inválido.");
        if(f.origem()!=null && !Set.of("loja","balcao").contains(f.origem()))throw new IllegalArgumentException("Origem inválida.");
        try(Connection c=ConexaoBanco.getConexao()){
            boolean equipe=Sessao.getIdUsuario()>0;
            if(equipe)Acesso.equipe(c,false);else Acesso.cliente(c,Sessao.getIdCliente());
            StringBuilder sql=new StringBuilder("SELECT p.id,p.data_pedido,cl.nome,p.origem,p.status,pg.status,p.total FROM pedidos p JOIN clientes cl ON cl.id=p.id_cliente JOIN pagamentos pg ON pg.id_pedido=p.id WHERE 1=1");
            List<Object> args=new ArrayList<>();
            if(!equipe){sql.append(" AND p.id_cliente=?");args.add(Sessao.getIdCliente());}
            if(f.inicio()!=null){sql.append(" AND p.data_pedido>=?");args.add(Timestamp.valueOf(f.inicio().atStartOfDay()));}
            if(f.fim()!=null){sql.append(" AND p.data_pedido<?");args.add(Timestamp.valueOf(f.fim().plusDays(1).atStartOfDay()));}
            if(f.cliente()!=null && !f.cliente().isBlank()){
                sql.append(" AND (strpos(lower(cl.nome),?)>0 OR strpos(lower(cl.email),?)>0)");
                String termo=f.cliente().trim().toLowerCase(Locale.ROOT);args.add(termo);args.add(termo);
            }
            if(f.status()!=null){sql.append(" AND p.status=?");args.add(f.status());}
            if(f.origem()!=null){sql.append(" AND p.origem=?");args.add(f.origem());}
            sql.append(" ORDER BY p.data_pedido DESC,p.id DESC");
            List<Resumo> lista=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement(sql.toString())){
                for(int i=0;i<args.size();i++)p.setObject(i+1,args.get(i));
                try(ResultSet r=p.executeQuery()){while(r.next())lista.add(new Resumo(r.getInt(1),r.getTimestamp(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),PagamentoSimulado.moeda(r.getBigDecimal(7))));}
            }return lista;
        }
    }

    /** Estado esperado impede um segundo clique ou tela desatualizada de avançar duas etapas. */
    public void avancar(int id,String esperado,String destino)throws SQLException{
        if(!StatusPedido.TODOS.contains(Objects.toString(esperado,"")) || !Objects.equals(StatusPedido.proximo(esperado),destino) || destino==null)
            throw new IllegalArgumentException("Transição de pedido inválida.");
        Transacao.executar(c->{
            Acesso.equipe(c,false);Estado estado=bloquear(c,id,true);
            if(!estado.status().equals(esperado))throw new IllegalArgumentException("O pedido mudou. Atualize a lista antes de continuar.");
            if(!estado.pagamento().equals("confirmado"))throw new IllegalArgumentException("Confirme o pagamento simulado antes de preparar o pedido.");
            atualizar(c,id,destino);registrar(c,id,esperado,destino,"Avanço da entrega simulada");return null;
        });
    }

    public void cancelar(int id,String motivo)throws SQLException{
        String razao=Validacao.texto(motivo,"Motivo do cancelamento",3,255);
        Transacao.executar(c->{
            boolean equipe=Sessao.getIdUsuario()>0;
            if(equipe)Acesso.equipe(c,false);else Acesso.cliente(c,Sessao.getIdCliente());
            Estado estado=bloquear(c,id,equipe);
            // Uma repetição autorizada não produz devolução ou histórico duplicado.
            if(estado.status().equals("cancelado"))return null;
            if(!StatusPedido.podeCancelar(estado.status(),equipe))throw new IllegalArgumentException(equipe?"Só é possível cancelar antes do envio.":"Você só pode cancelar enquanto o pedido estiver aberto. Procure a equipe após o início do preparo.");
            try(PreparedStatement p=c.prepareStatement("SELECT id_produto,SUM(quantidade)::bigint FROM itens_pedido WHERE id_pedido=? GROUP BY id_produto ORDER BY id_produto")){
                p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                    boolean encontrou=false;
                    while(r.next()){
                        encontrou=true;long qtd=r.getLong(2);
                        if(qtd>Integer.MAX_VALUE)throw new IllegalArgumentException("Quantidade excede o limite do estoque.");
                        new EstoqueDAO().devolver(c,id,r.getInt(1),(int)qtd,"Cancelamento do pedido #"+id+": "+razao);
                    }
                    if(!encontrou)throw new IllegalArgumentException("Pedido sem itens; cancelamento requer conferência da equipe.");
                }
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE pagamentos SET status='cancelado',cancelado_em=CURRENT_TIMESTAMP WHERE id_pedido=?")){p.setInt(1,id);if(p.executeUpdate()!=1)throw new SQLException("Pagamento ausente.");}
            atualizar(c,id,"cancelado");registrar(c,id,estado.status(),"cancelado",razao);return null;
        });
    }

    private static Estado bloquear(Connection c,int id,boolean equipe)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT p.status,pg.status,p.id_cliente FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.id=? "+(equipe?"":"AND p.id_cliente=? ")+"FOR UPDATE OF p,pg")){
            p.setInt(1,id);if(!equipe)p.setInt(2,Sessao.getIdCliente());
            try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Pedido não encontrado.");return new Estado(r.getString(1),r.getString(2),r.getInt(3));}
        }
    }
    private static void atualizar(Connection c,int id,String status)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("UPDATE pedidos SET status=? WHERE id=?")){p.setString(1,status);p.setInt(2,id);if(p.executeUpdate()!=1)throw new SQLException("Pedido não alterado.");}
    }
    static void registrar(Connection c,int id,String anterior,String novo,String motivo)throws SQLException{
        String responsavel;
        boolean equipe=Sessao.getIdUsuario()>0;
        try(PreparedStatement p=c.prepareStatement(equipe?"SELECT nome FROM usuarios WHERE id=?":"SELECT nome FROM clientes WHERE id=?")){
            p.setInt(1,equipe?Sessao.getIdUsuario():Sessao.getIdCliente());try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Responsável inexistente.");responsavel=r.getString(1);}
        }
        try(PreparedStatement p=c.prepareStatement("INSERT INTO historico_pedidos(id_pedido,status_anterior,status_novo,motivo,responsavel,id_usuario,id_cliente) VALUES(?,?,?,?,?,?,?)")){
            p.setInt(1,id);p.setString(2,anterior);p.setString(3,novo);p.setString(4,motivo);p.setString(5,responsavel);
            if(equipe)p.setInt(6,Sessao.getIdUsuario());else p.setNull(6,Types.INTEGER);
            if(equipe)p.setNull(7,Types.INTEGER);else p.setInt(7,Sessao.getIdCliente());
            if(p.executeUpdate()!=1)throw new SQLException("Histórico não registrado.");
        }
    }
    // Consulta interna: detalhes já validou titular/equipe usando a mesma conexão.
    static String historico(Connection c,int id)throws SQLException{
        StringBuilder texto=new StringBuilder("\nAcompanhamento da entrega simulada:\n");
        try(PreparedStatement p=c.prepareStatement("SELECT data_evento,status_anterior,status_novo,motivo,responsavel FROM historico_pedidos WHERE id_pedido=? ORDER BY id")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())texto.append(Datas.formatar(r.getTimestamp(1))).append(" | ").append(r.getString(2)==null?"Registro inicial":StatusPedido.nome(r.getString(2))).append(" → ").append(StatusPedido.nome(r.getString(3))).append("\n  ").append(r.getString(5)).append(": ").append(r.getString(4)).append("\n");}
        }return texto.toString();
    }
}
