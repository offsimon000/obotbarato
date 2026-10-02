import dao.*;
import model.*;
import util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Regras críticas de gestão verificadas no PostgreSQL isolado, inclusive falhas e disputas. */
public final class GestaoECancelamento {
    private static final GestaoPedidoDAO gestao=new GestaoPedidoDAO();
    private static final CompraDAO compras=new CompraDAO();
    private static Usuario admin,vendedor;
    private static Cliente cliente,outro;
    private static Produto produto,segundo;
    private static int cenarios;
    public static void main(String[] args)throws Exception{
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");
        admin=new UsuarioDAO().autenticar("admin","admin123");vendedor=new UsuarioDAO().autenticar("vendedor","vendedor123");
        Sessao.iniciar(admin);produto=produto("Gestao Primeiro",60);segundo=produto("Gestao Segundo",10);
        exigir(new ClienteLojaDAO().cadastrar("Cliente Gestao",null,"gestao@teste.local",null,null,"senha12345")==null,"Cadastro falhou.");
        exigir(new ClienteLojaDAO().cadastrar("Outro Gestao",null,"outrogestao@teste.local",null,null,"senha12345")==null,"Cadastro falhou.");
        cliente=new ClienteLojaDAO().autenticar("gestao@teste.local","senha12345");outro=new ClienteLojaDAO().autenticar("outrogestao@teste.local","senha12345");
        Sessao.iniciarCliente(cliente);int pago=compra("pix",2);
        exigir(status(pago).equals("aberto") && pagamento(pago).equals("confirmado"),"Pagamento confundido com entrega.");
        exigir(numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+pago)==1 && numero("SELECT COUNT(*) FROM movimentos_estoque WHERE id_pedido="+pago+" AND tipo='venda'")==1,"Auditoria inicial ausente.");ok("Compra paga nasce aberta, com histórico e movimento vinculados ao pedido");

        negar(()->gestao.avancar(pago,"aberto","em_preparo"),SQLException.class);
        Sessao.iniciar(vendedor);negar(()->gestao.avancar(pago,"aberto","enviado"),IllegalArgumentException.class);
        gestao.avancar(pago,"aberto","em_preparo");negar(()->gestao.avancar(pago,"aberto","em_preparo"),IllegalArgumentException.class);
        gestao.avancar(pago,"em_preparo","enviado");gestao.avancar(pago,"enviado","entregue");
        negar(()->gestao.avancar(pago,"entregue","aberto"),IllegalArgumentException.class);
        exigir(numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+pago)==4 && numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+pago+" AND id_usuario="+vendedor.getId())==3,"Responsável/histórico incorreto.");ok("Equipe segue aberto → preparo → enviado → entregue, sem saltos, retornos ou avanço repetido");

        int saldo=new EstoqueDAO().getQuantidade(produto.getId());negar(()->gestao.cancelar(pago,"Cancelar entregue"),IllegalArgumentException.class);exigir(new EstoqueDAO().getQuantidade(produto.getId())==saldo,"Entregue devolveu estoque.");
        Sessao.iniciarCliente(cliente);int enviado=compra("credito",1);Sessao.iniciar(vendedor);gestao.avancar(enviado,"aberto","em_preparo");gestao.avancar(enviado,"em_preparo","enviado");negar(()->gestao.cancelar(enviado,"Cancelar enviado"),IllegalArgumentException.class);ok("Enviados e entregues não podem ser cancelados por este fluxo");

        Sessao.iniciarCliente(cliente);int boleto=compra("boleto",1);Sessao.iniciar(vendedor);negar(()->gestao.avancar(boleto,"aberto","em_preparo"),IllegalArgumentException.class);
        Sessao.iniciar(admin);compras.confirmarPagamento(boleto);compras.confirmarPagamento(boleto);
        exigir(status(boleto).equals("aberto") && numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+boleto)==2,"Confirmação alterou entrega ou duplicou histórico.");
        gestao.avancar(boleto,"aberto","em_preparo");ok("Boleto pendente bloqueia preparo; confirmação muda só pagamento e registra evento uma vez");

        Sessao.iniciarCliente(cliente);int cancelar=compra("pix",3);int antes=new EstoqueDAO().getQuantidade(produto.getId());gestao.cancelar(cancelar,"Cliente desistiu da compra");gestao.cancelar(cancelar,"Repetição do cancelamento");
        exigir(status(cancelar).equals("cancelado") && pagamento(cancelar).equals("cancelado") && new EstoqueDAO().getQuantidade(produto.getId())==antes+3,"Cancelamento incoerente.");
        exigir(numero("SELECT COUNT(*) FROM movimentos_estoque WHERE id_pedido="+cancelar+" AND tipo='devolucao'")==1 && numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+cancelar)==2,"Repetição duplicada.");
        exigir(numero("SELECT COUNT(*) FROM pagamentos WHERE id_pedido="+cancelar+" AND confirmado_em IS NOT NULL AND cancelado_em IS NOT NULL")==1,"Perdeu data da confirmação/cancelamento.");ok("Cliente cancela aberto, preserva valores/datas e recebe uma devolução única auditada");

        int pendente=compra("boleto",1);gestao.cancelar(pendente,"Boleto não será usado");Sessao.iniciar(admin);negar(()->compras.confirmarPagamento(pendente),IllegalArgumentException.class);exigir(pagamento(pendente).equals("cancelado"),"Pagamento cancelado reaberto.");ok("Cancelamento de boleto pendente impede confirmação posterior");

        Sessao.iniciarCliente(cliente);int preparo=compra("debito",1);Sessao.iniciar(vendedor);gestao.avancar(preparo,"aberto","em_preparo");Sessao.iniciarCliente(cliente);negar(()->gestao.cancelar(preparo,"Cancelar em preparo"),IllegalArgumentException.class);
        Sessao.iniciar(vendedor);gestao.cancelar(preparo,"Equipe cancelou antes do envio");exigir(status(preparo).equals("cancelado"),"Equipe não cancelou preparo.");ok("Cliente não cancela em preparo; equipe cancela antes do envio");

        Sessao.iniciarCliente(cliente);int privado=compra("credito",1);Sessao.iniciarCliente(outro);
        negar(()->gestao.cancelar(privado,"Pedido alheio"),IllegalArgumentException.class);negar(()->compras.detalhes(privado),IllegalArgumentException.class);
        exigir(gestao.listar(null).isEmpty(),"Consulta expôs outro cliente.");Sessao.encerrar();negar(()->gestao.listar(null),SQLException.class);negar(()->gestao.cancelar(privado,"Sem sessão"),SQLException.class);ok("Consulta, detalhes e cancelamento exigem sessão e titularidade real");

        Sessao.iniciarCliente(cliente);int invalido=compra("pix",1);negar(()->gestao.cancelar(invalido," "),IllegalArgumentException.class);negar(()->gestao.cancelar(invalido,"x".repeat(256)),IllegalArgumentException.class);exigir(status(invalido).equals("aberto"),"Motivo inválido cancelou.");ok("Motivo obrigatório e limitado, sem alterações em pedidos inválidos");

        int simultaneo=compra("pix",2);antes=new EstoqueDAO().getQuantidade(produto.getId());paralelo(()->{gestao.cancelar(simultaneo,"Cancelamento simultâneo");return true;},()->{gestao.cancelar(simultaneo,"Cancelamento simultâneo");return true;});
        exigir(new EstoqueDAO().getQuantidade(produto.getId())==antes+2 && numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+simultaneo)==2,"Cancelamento simultâneo duplicado.");ok("Duas solicitações simultâneas devolvem estoque apenas uma vez");

        int corrida=compra("pix",1);Sessao.iniciar(vendedor);
        List<Boolean> resultados=paralelo(()->tentar(()->gestao.avancar(corrida,"aberto","em_preparo")),()->tentar(()->gestao.avancar(corrida,"aberto","em_preparo")));
        exigir(resultados.get(0)^resultados.get(1),"Dois avanços aceitos.");exigir(status(corrida).equals("em_preparo") && numero("SELECT COUNT(*) FROM historico_pedidos WHERE id_pedido="+corrida)==2,"Avanço simultâneo pulou etapa.");ok("Duas telas avançando o mesmo estado produzem uma única transição");

        Sessao.iniciarCliente(cliente);int cancelEnvio=compra("pix",1);Sessao.iniciar(vendedor);gestao.avancar(cancelEnvio,"aberto","em_preparo");antes=new EstoqueDAO().getQuantidade(produto.getId());
        resultados=paralelo(()->tentar(()->gestao.cancelar(cancelEnvio,"Disputa com envio")),()->tentar(()->gestao.avancar(cancelEnvio,"em_preparo","enviado")));
        exigir(resultados.get(0)^resultados.get(1),"Envio e cancelamento aceitos juntos.");exigir(new EstoqueDAO().getQuantidade(produto.getId())==antes+(status(cancelEnvio).equals("cancelado")?1:0),"Disputa devolveu estoque indevidamente.");ok("Disputa entre envio e cancelamento termina num único estado coerente");

        Sessao.iniciarCliente(cliente);int cancelPag=compra("boleto",1);Sessao.iniciar(admin);antes=new EstoqueDAO().getQuantidade(produto.getId());
        paralelo(()->{gestao.cancelar(cancelPag,"Disputa com confirmação");return true;},()->tentar(()->compras.confirmarPagamento(cancelPag)));
        exigir(status(cancelPag).equals("cancelado") && pagamento(cancelPag).equals("cancelado") && new EstoqueDAO().getQuantidade(produto.getId())==antes+1,"Pagamento ressuscitou cancelamento.");ok("Disputa confirmação/cancelamento nunca deixa pedido cancelado com pagamento confirmado");

        Sessao.iniciarCliente(cliente);int multiplo=compras.confirmar(cliente.getId(),List.of(item(produto,2),item(segundo,2)),"pix","Cliente Entrega","11999990000","Rua Teste, 100",UUID.randomUUID()).id();
        int saldo1=new EstoqueDAO().getQuantidade(produto.getId()),saldo2=new EstoqueDAO().getQuantidade(segundo.getId());long mov=numero("SELECT COUNT(*) FROM movimentos_estoque");
        sql("CREATE FUNCTION teste_cancelamento_falha() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.status_novo='cancelado' THEN RAISE EXCEPTION 'falha de historico proposital'; END IF; RETURN NEW; END $$");
        sql("CREATE TRIGGER teste_cancelamento BEFORE INSERT ON historico_pedidos FOR EACH ROW EXECUTE FUNCTION teste_cancelamento_falha()");
        try{negar(()->gestao.cancelar(multiplo,"Falha após duas devoluções"),SQLException.class);exigir(status(multiplo).equals("aberto") && pagamento(multiplo).equals("confirmado") && numero("SELECT COUNT(*) FROM movimentos_estoque")==mov && new EstoqueDAO().getQuantidade(produto.getId())==saldo1 && new EstoqueDAO().getQuantidade(segundo.getId())==saldo2,"Cancelamento deixou alterações parciais.");}
        finally{sql("DROP TRIGGER teste_cancelamento ON historico_pedidos");sql("DROP FUNCTION teste_cancelamento_falha()");}ok("Falha no histórico após devolver dois produtos desfaz estoque, movimentos, pedido e pagamento");

        Sessao.iniciar(admin);new ProdutoDAO().excluir(segundo.getId());Sessao.iniciarCliente(cliente);gestao.cancelar(multiplo,"Devolver também produto desativado");exigir(new EstoqueDAO().getQuantidade(segundo.getId())==saldo2+2,"Inativo não devolvido.");ok("Cancelamento devolve todos os itens, inclusive produtos desativados após a compra");

        Sessao.iniciar(admin);LocalDate hoje=LocalDate.parse(texto("SELECT CURRENT_DATE::text"));
        var filtrados=gestao.listar(new GestaoPedidoDAO.Filtro(hoje,hoje,"GESTAO@TESTE.LOCAL","cancelado","loja"));exigir(!filtrados.isEmpty() && filtrados.stream().allMatch(p->p.status().equals("cancelado") && p.origem().equals("loja")),"Filtros não combinam.");
        exigir(gestao.listar(new GestaoPedidoDAO.Filtro(hoje.plusDays(1),hoje.plusDays(1),null,null,null)).isEmpty(),"Filtro futuro inclui pedido atual.");
        negar(()->gestao.listar(new GestaoPedidoDAO.Filtro(hoje,hoje.minusDays(1),null,null,null)),IllegalArgumentException.class);
        exigir(gestao.listar(new GestaoPedidoDAO.Filtro(null,null,"' OR 1=1 --",null,null)).isEmpty(),"Busca interpretou SQL.");
        String detalhes=compras.detalhes(multiplo);exigir(detalhes.contains("Devolver também") && detalhes.contains("Cliente Gestao") && detalhes.contains("Rua Teste") && detalhes.contains("Cancelado"),"Detalhes sem histórico/entrega/responsável.");ok("Filtros combinados de data/cliente/status/origem e detalhes com histórico seguro");

        BigDecimal esperado=new BigDecimal(texto("SELECT COALESCE(SUM(p.total),0)::text FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE p.status<>'cancelado' AND pg.status='confirmado' AND DATE(p.data_pedido)=CURRENT_DATE"));
        var relatorio=new PedidoDAO().relatorioVendasPorPeriodo(hoje.toString(),hoje.toString());exigir(new BigDecimal(relatorio.get(0)[2].replace(",",".")).compareTo(esperado)==0,"Faturamento inclui cancelados/pendentes ou exclui preparo.");
        int venda;Pedido balc=new Pedido();balc.setIdCliente(cliente.getId());balc.setStatus("entregue");balc.adicionarItem(item(produto,1));new PedidoDAO().salvar(balc);venda=balc.getId();exigir(status(venda).equals("entregue") && pagamento(venda).equals("confirmado"),"Retirada não concluída.");
        negar(()->gestao.cancelar(venda,"Retirada já entregue"),IllegalArgumentException.class);ok("Faturamento exclui cancelados/pendentes; venda no balcão registra retirada entregue e pagamento simulado");
        System.out.println("APROVADO: "+cenarios+" cenários de gestão e cancelamento em PostgreSQL real.");
    }
    private static Produto produto(String nome,int qtd)throws Exception{Produto p=new Produto();p.setNome(nome);p.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());p.setPreco(new BigDecimal("10.00"));p.setEstoqueInicial(qtd);p.setAtivo(true);new ProdutoDAO().salvar(p);return p;}
    private static ItemPedido item(Produto p,int qtd){ItemPedido i=new ItemPedido();i.setIdProduto(p.getId());i.setNomeProduto(p.getNome());i.setQuantidade(qtd);i.setPrecoUnitario(p.getPrecoDecimal());return i;}
    private static int compra(String metodo,int qtd)throws SQLException{return compras.confirmar(cliente.getId(),List.of(item(produto,qtd)),metodo,"Cliente Entrega","11999990000","Rua Teste, 100",UUID.randomUUID()).id();}
    private static String status(int id)throws SQLException{return texto("SELECT status FROM pedidos WHERE id="+id);}
    private static String pagamento(int id)throws SQLException{return texto("SELECT status FROM pagamentos WHERE id_pedido="+id);}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){exigir(r.next(),"Consulta sem resultado.");return r.getString(1);}}
    private static long numero(String s)throws SQLException{return Long.parseLong(texto(s));}
    private static void sql(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement()){p.execute(s);}}
    private static List<Boolean> paralelo(Callable<Boolean> a,Callable<Boolean> b)throws Exception{ExecutorService ex=Executors.newFixedThreadPool(2);CountDownLatch inicio=new CountDownLatch(1);try{Future<Boolean> fa=ex.submit(()->{inicio.await();return a.call();}),fb=ex.submit(()->{inicio.await();return b.call();});inicio.countDown();return List.of(fa.get(30,TimeUnit.SECONDS),fb.get(30,TimeUnit.SECONDS));}finally{ex.shutdownNow();}}
    private static boolean tentar(Operacao op)throws Exception{try{op.executar();return true;}catch(IllegalArgumentException e){return false;}}
    @FunctionalInterface private interface Operacao{void executar()throws Exception;}
    private static void negar(Operacao op,Class<? extends Throwable> tipo)throws Exception{try{op.executar();}catch(Throwable e){if(tipo.isInstance(e))return;throw new AssertionError("Erro diferente do esperado.",e);}throw new AssertionError("Operação indevida aceita.");}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void ok(String s){cenarios++;System.out.println("OK "+cenarios+": "+s);}
}
