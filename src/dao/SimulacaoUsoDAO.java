package dao;

import model.*;
import util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Carga fictícia manual e atômica. UUIDs fixos impedem duplicação ou reposição ao repetir. */
public final class SimulacaoUsoDAO {
    public static final String SENHA="demo12345";
    private static final String DOMINIO="@uso.obotbarato.test";
    private static final String CUPOM="DEMO_DIA10";
    private static final String[] NOMES={"Ana Martins","Bruno Almeida","Carla Ribeiro","Daniel Costa","Elisa Santos","Felipe Rocha","Gabriela Lima","Henrique Souza","Isabela Freitas","João Ferreira","Larissa Oliveira","Marcos Silva","Natália Lopes","Otávio Mendes","Paula Castro","Rafael Nunes","Sofia Pereira","Thiago Barbosa","Vanessa Dias","William Teixeira"};
    public record Resumo(int clientesNovos,int pedidosNovos,LocalDate inicio,LocalDate fim,int pedidos,int confirmados,int pendentes,int cancelados,int avaliacoes,int estoqueCritico,BigDecimal faturamento) {}
    private static final class Sku {
        int id,saldo,minimo;String nome;BigDecimal preco;
        Sku(ResultSet r)throws SQLException{id=r.getInt(1);nome=r.getString(2);preco=r.getBigDecimal(3);saldo=r.getInt(4);minimo=r.getInt(5);}
    }
    private record Linha(Sku produto,int quantidade) {}
    private record Plano(int indice,LocalDateTime data,String destino,String metodo,boolean balcao) {}
    public static UUID chave(int indice){return UUID.nameUUIDFromBytes(("obotbarato:uso-diario:v1:"+indice).getBytes(StandardCharsets.UTF_8));}
    public static String email(int indice){return String.format(Locale.ROOT,"cliente%02d",indice+1)+DOMINIO;}

    public Resumo instalar(LocalDate fim)throws SQLException {
        if(fim==null || fim.isAfter(LocalDate.now()) || fim.getYear()<2000)throw new IllegalArgumentException("Informe uma data de referência válida, sem dias futuros.");
        // Recusa a conta não autorizada antes de alternar identidades na carga.
        Acesso.admin();
        Usuario operador=Sessao.getUsuario();
        try{return Transacao.executar(c->{
            Acesso.equipe(c,true);
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){r.next();if(!Set.of("obotbarato","obotbarato_test").contains(r.getString(1)))throw new IllegalArgumentException("Carga permitida somente na base local ou de teste.");}
            try(Statement s=c.createStatement()){s.execute("SELECT pg_advisory_xact_lock(726287)");s.execute("SELECT pg_advisory_xact_lock_shared(726282)");}
            int existentes=(int)numero(c,"SELECT COUNT(*) FROM pedidos WHERE chave_compra=ANY(?)",chaves(c));
            if(existentes==60)return resumo(c,0,0);
            if(existentes!=0)throw new IllegalArgumentException("Carga fictícia incompleta encontrada. Dados preservados; confira o backup antes de repetir.");
            if(numero(c,"SELECT COUNT(*) FROM clientes WHERE right(email,?)=?",DOMINIO.length(),DOMINIO)>0 || numero(c,"SELECT COUNT(*) FROM cupons WHERE codigo=?",CUPOM)>0)throw new IllegalArgumentException("Identificadores da demonstração já estão em uso. Nenhum dado foi alterado.");
            List<Sku> produtos=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement("SELECT pr.id,pr.nome,pr.preco,e.quantidade,e.estoque_minimo FROM produtos pr JOIN categorias ct ON ct.id=pr.id_categoria JOIN estoque e ON e.id_produto=pr.id WHERE pr.ativo AND ct.ativo AND pr.preco>0 ORDER BY pr.id FOR UPDATE OF pr,e");ResultSet r=p.executeQuery()){while(r.next())produtos.add(new Sku(r));}
            if(produtos.size()<10)throw new IllegalArgumentException("Instale o catálogo de apresentação antes da simulação (ao menos 10 produtos ativos).");
            List<Sku> destaques=produtos.stream().filter(p->p.saldo>=p.minimo+6L).limit(3).toList();
            List<Sku> normais=produtos.stream().filter(p->!destaques.contains(p)).toList();
            if(destaques.size()!=3 || normais.stream().mapToLong(p->p.saldo).sum()<300)throw new IllegalArgumentException("Saldo insuficiente para simular 60 pedidos e conservar o catálogo. Reponha estoque antes de repetir.");
            List<Cliente> clientes=new ArrayList<>();
            for(int i=0;i<20;i++){
                Cliente cl=new Cliente();cl.setNome(NOMES[i]+" (Demo)");cl.setEmail(email(i));cl.setTelefone("1199999"+String.format(Locale.ROOT,"%04d",100+i));cl.setEndereco("Rua Demonstração (fictícia), "+(100+i)+", São Paulo/SP");Validacao.cliente(cl);
                cl.setId(inserir(c,"INSERT INTO clientes(nome,email,telefone,endereco,senha,criado_por) VALUES(?,?,?,?,?,?) RETURNING id",cl.getNome(),cl.getEmail(),cl.getTelefone(),cl.getEndereco(),SenhaCripto.hashear(SENHA),operador.getId()));cl.setAcessoLoja(true);clientes.add(cl);
            }
            Cliente demo=null;try(PreparedStatement p=c.prepareStatement("SELECT id,nome,email,telefone,endereco FROM clientes WHERE email='cliente@demo.local' AND ativo AND senha IS NOT NULL");ResultSet r=p.executeQuery()){if(r.next())demo=new Cliente(r.getInt(1),r.getString(2),null,r.getString(3),r.getString(4),r.getString(5));}
            int cupom=inserir(c,"INSERT INTO cupons(codigo,percentual,inicio,fim,criado_por,atualizado_por) VALUES(?,10,?,?,?,?) RETURNING id",CUPOM,fim.minusDays(27),fim.plusDays(28),operador.getId(),operador.getId());
            List<Plano> planos=new ArrayList<>();LocalDateTime agora=LocalDateTime.now();
            for(int i=0;i<60;i++){
                String destino=i<30?"entregue":i<36?"enviado":i<42?"em_preparo":i<54?"aberto":"cancelado";
                int dias=i<30?27-i%26:i<36?2+i%3:i<42?1+i%2:i<54?i%2:4+i%13;
                LocalDateTime data=fim.minusDays(dias).atTime(8+i%8,10+i%40);if(data.isAfter(agora.minusHours(5)))data=agora.minusHours(5);
                boolean balcao=i<12;String metodo=balcao?"dinheiro":i>=48 && i<54?"boleto":i>=42 && i<48?new String[]{"pix","debito","credito"}[i%3]:new String[]{"pix","debito","credito","boleto"}[i%4];
                planos.add(new Plano(i,data,destino,metodo,balcao));
            }
            planos.sort(Comparator.comparing(Plano::data).thenComparingInt(Plano::indice));Random sorteio=new Random(20261010);int[] restantes={6,6,6};
            for(Plano plano:planos){
                int i=plano.indice();Cliente cl=demo!=null && (i%12==0 || i==30 || i==42 || i==54)?demo:clientes.get(i%20);List<Linha> linhas=new ArrayList<>();
                if(i<18){int grupo=i%3;Sku popular=destaques.get(grupo);int falta=popular.saldo-popular.minimo;int qtd=(int)Math.ceil(falta/(double)restantes[grupo]--);if(qtd>0)linhas.add(new Linha(popular,qtd));}
                int quantidadeItens=2+i%2;
                while(linhas.size()<quantidadeItens){List<Sku> disponiveis=normais.stream().filter(p->p.saldo>0 && linhas.stream().noneMatch(l->l.produto()==p)).toList();if(disponiveis.isEmpty())throw new IllegalArgumentException("Saldo insuficiente para completar a carga. Operação desfeita.");Sku p=disponiveis.get(sorteio.nextInt(disponiveis.size()));linhas.add(new Linha(p,Math.min(p.saldo,1+sorteio.nextInt(2))));}
                Sessao.iniciar(operador);boolean usaCupom=!plano.balcao() && i%3==0;BigDecimal bruto=linhas.stream().map(l->l.produto().preco.multiply(BigDecimal.valueOf(l.quantidade()))).reduce(new BigDecimal("0.00"),BigDecimal::add);
                var v=DescontosCompra.calcular(bruto,plano.metodo(),usaCupom?new BigDecimal("10"):null);Timestamp data=Timestamp.valueOf(plano.data());
                int pedido=inserir(c,"INSERT INTO pedidos(id_cliente,data_pedido,status,origem,chave_compra,registrado_por,subtotal,desconto,total,destinatario,telefone_entrega,endereco_entrega,id_cupom,codigo_cupom,percentual_cupom,desconto_cupom,desconto_pagamento) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id",cl.getId(),data,plano.balcao()?"entregue":"aberto",plano.balcao()?"balcao":"loja",chave(i),plano.balcao()?operador.getId():null,bruto,v.desconto(),v.total(),plano.balcao()?null:cl.getNome(),plano.balcao()?null:cl.getTelefone(),plano.balcao()?null:cl.getEndereco(),usaCupom?cupom:null,usaCupom?CUPOM:null,usaCupom?new BigDecimal("10"):BigDecimal.ZERO,v.cupom(),v.pagamento());
                if(!plano.balcao())Sessao.iniciarCliente(cl);
                for(Linha l:linhas){executar(c,"INSERT INTO itens_pedido(id_pedido,id_produto,quantidade,preco_unitario) VALUES(?,?,?,?)",pedido,l.produto().id,l.quantidade(),l.produto().preco);mover(c,l,pedido,false,plano.data());}
                boolean pendente=plano.metodo().equals("boleto");executar(c,"INSERT INTO pagamentos(id_pedido,metodo,status,criado_em,confirmado_em) VALUES(?,?,?,?,?)",pedido,plano.metodo(),pendente?"pendente":"confirmado",data,pendente?null:data);
                evento(c,pedido,null,plano.balcao()?"entregue":"aberto","Simulação uso diário: "+(plano.balcao()?"retirada no balcão, dinheiro confirmado":"compra pela loja, pagamento "+(pendente?"pendente":"confirmado")),plano.data());
                Sessao.iniciar(operador);LocalDateTime quando=plano.data().plusMinutes(30);
                if(pendente && !plano.destino().equals("aberto") && !plano.destino().equals("cancelado")){executar(c,"UPDATE pagamentos SET status='confirmado',confirmado_em=?,confirmado_por=? WHERE id_pedido=?",Timestamp.valueOf(quando),operador.getId(),pedido);evento(c,pedido,"aberto","aberto","Simulação uso diário: boleto confirmado pelo administrador",quando);}
                if(plano.destino().equals("cancelado")){
                    for(Linha l:linhas)mover(c,l,pedido,true,quando);
                    executar(c,"UPDATE pagamentos SET status='cancelado',cancelado_em=? WHERE id_pedido=?",Timestamp.valueOf(quando),pedido);executar(c,"UPDATE pedidos SET status='cancelado' WHERE id=?",pedido);evento(c,pedido,"aberto","cancelado","Simulação uso diário: cliente desistiu antes do envio",quando);
                }else if(!plano.balcao()){
                    String estado="aberto";while(!estado.equals(plano.destino())){String proximo=StatusPedido.proximo(estado);if(proximo==null)throw new IllegalArgumentException("Plano de entrega inválido.");quando=quando.plusHours(1);executar(c,"UPDATE pedidos SET status=? WHERE id=?",proximo,pedido);evento(c,pedido,estado,proximo,"Simulação uso diário: avanço da entrega",quando);estado=proximo;}
                    if(estado.equals("entregue")){Linha l=linhas.get(0);executar(c,"INSERT INTO avaliacoes(id_cliente,id_produto,id_pedido,nota,comentario,criada_em,atualizada_em) VALUES(?,?,?,?,?,?,?) ON CONFLICT(id_cliente,id_produto) DO NOTHING",cl.getId(),l.produto().id,pedido,3+i%3,new String[]{"Simulação: gostei do produto e da embalagem.","Simulação: compra fácil e entrega acompanhada.","Simulação: produto bom, voltaria a comprar."}[i%3],Timestamp.valueOf(quando.plusMinutes(20)),Timestamp.valueOf(quando.plusMinutes(20)));}
                }
            }
            Sessao.iniciar(operador);
            for(int i=0;i<20;i++){executar(c,"INSERT INTO favoritos(id_cliente,id_produto) VALUES(?,?),(?,?) ON CONFLICT DO NOTHING",clientes.get(i).getId(),destaques.get(i%3).id,clientes.get(i).getId(),normais.get(i%normais.size()).id);}
            executar(c,"INSERT INTO leituras_notificacoes(id_notificacao,id_usuario,lida_em) SELECT n.id,?,n.criada_em+INTERVAL '30 minutes' FROM notificacoes n JOIN pedidos p ON p.id=n.id_pedido WHERE n.destino='equipe' AND p.chave_compra=ANY(?) AND n.criada_em<? ON CONFLICT DO NOTHING",operador.getId(),chaves(c),Timestamp.valueOf(fim.minusDays(5).atStartOfDay()));
            executar(c,"INSERT INTO leituras_notificacoes(id_notificacao,id_cliente,lida_em) SELECT n.id,n.id_cliente,n.criada_em+INTERVAL '30 minutes' FROM notificacoes n JOIN pedidos p ON p.id=n.id_pedido WHERE n.destino='cliente' AND p.chave_compra=ANY(?) AND n.criada_em<? ON CONFLICT DO NOTHING",chaves(c),Timestamp.valueOf(fim.minusDays(5).atStartOfDay()));
            return resumo(c,20,60);
        });}finally{if(operador!=null)Sessao.iniciar(operador);else Sessao.encerrar();}
    }
    private static void mover(Connection c,Linha linha,int pedido,boolean devolucao,LocalDateTime data)throws SQLException{
        Sku p=linha.produto();long episodio=numero(c,"SELECT episodio_alerta FROM estoque WHERE id_produto=?",p.id);EstoqueDAO estoque=new EstoqueDAO();
        if(devolucao){estoque.devolver(c,pedido,p.id,linha.quantidade(),"Simulação uso diário: cancelamento antes do envio");p.saldo+=linha.quantidade();}else{estoque.saida(c,p.id,linha.quantidade(),pedido);p.saldo-=linha.quantidade();}
        executar(c,"UPDATE movimentos_estoque SET data_movimento=? WHERE id_pedido=? AND id_produto=? AND tipo=?",Timestamp.valueOf(data),pedido,p.id,devolucao?"devolucao":"venda");
        long novo=numero(c,"SELECT episodio_alerta FROM estoque WHERE id_produto=?",p.id);if(novo>episodio)executar(c,"UPDATE notificacoes SET criada_em=? WHERE chave=?",Timestamp.valueOf(data),"estoque:"+p.id+":"+novo);
    }
    private static void evento(Connection c,int pedido,String antes,String depois,String motivo,LocalDateTime data)throws SQLException{
        GestaoPedidoDAO.registrar(c,pedido,antes,depois,motivo);
        executar(c,"UPDATE historico_pedidos SET data_evento=? WHERE id=(SELECT MAX(id) FROM historico_pedidos WHERE id_pedido=?)",Timestamp.valueOf(data),pedido);
        executar(c,"UPDATE notificacoes SET criada_em=? WHERE chave IN (SELECT 'historico:'||MAX(id)||':cliente' FROM historico_pedidos WHERE id_pedido=? UNION ALL SELECT 'historico:'||MAX(id)||':equipe' FROM historico_pedidos WHERE id_pedido=?)",Timestamp.valueOf(data),pedido,pedido);
    }
    private static Resumo resumo(Connection c,int clientes,int pedidos)throws SQLException{
        String sql="SELECT MIN(p.data_pedido)::date,MAX(p.data_pedido)::date,COUNT(*),COUNT(*) FILTER(WHERE p.status<>'cancelado' AND pg.status='confirmado'),COUNT(*) FILTER(WHERE pg.status='pendente'),COUNT(*) FILTER(WHERE p.status='cancelado'),COALESCE(SUM(p.total) FILTER(WHERE p.status<>'cancelado' AND pg.status='confirmado'),0) FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.chave_compra=ANY(?)";
        try(PreparedStatement p=preparar(c,sql,chaves(c));ResultSet r=p.executeQuery()){r.next();return new Resumo(clientes,pedidos,r.getDate(1).toLocalDate(),r.getDate(2).toLocalDate(),r.getInt(3),r.getInt(4),r.getInt(5),r.getInt(6),(int)numero(c,"SELECT COUNT(*) FROM avaliacoes a JOIN pedidos p ON p.id=a.id_pedido WHERE p.chave_compra=ANY(?)",chaves(c)),(int)numero(c,"SELECT COUNT(*) FROM estoque e JOIN produtos p ON p.id=e.id_produto WHERE p.ativo AND e.quantidade<=e.estoque_minimo"),r.getBigDecimal(7));}
    }
    private static java.sql.Array chaves(Connection c)throws SQLException{return c.createArrayOf("uuid",java.util.stream.IntStream.range(0,60).mapToObj(SimulacaoUsoDAO::chave).toArray(UUID[]::new));}
    private static PreparedStatement preparar(Connection c,String sql,Object... args)throws SQLException{PreparedStatement p=c.prepareStatement(sql);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;}
    private static int inserir(Connection c,String sql,Object...args)throws SQLException{try(PreparedStatement p=preparar(c,sql,args);ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Registro não criado.");return r.getInt(1);}}
    private static void executar(Connection c,String sql,Object...args)throws SQLException{try(PreparedStatement p=preparar(c,sql,args)){p.executeUpdate();}}
    private static long numero(Connection c,String sql,Object...args)throws SQLException{try(PreparedStatement p=preparar(c,sql,args);ResultSet r=p.executeQuery()){r.next();return r.getLong(1);}}
}
