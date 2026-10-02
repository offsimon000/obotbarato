import dao.*;
import model.*;
import util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public final class CompraEPagamento {
    private static int cenarios;
    private static Usuario admin;
    private static Cliente cliente;
    private static int categoria;
    private static final CompraDAO compras=new CompraDAO();
    public static void main(String[] args)throws Exception{
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");
        admin=new UsuarioDAO().autenticar("admin","admin123");cliente=new ClienteLojaDAO().autenticar("cliente@demo.local","cliente123");categoria=(int)numero("SELECT MIN(id) FROM categorias WHERE ativo");
        exigir(PagamentoSimulado.desconto(new BigDecimal("99.99"),"pix").toPlainString().equals("10.00"),"PIX arredondamento.");
        exigir(PagamentoSimulado.total(new BigDecimal("99.99"),"debito").toPlainString().equals("94.99"),"Débito arredondamento.");negar(()->PagamentoSimulado.percentual(null),IllegalArgumentException.class);ok("Descontos calculados em decimal e arredondados para centavos");

        Produto p=produto("Compra Parte Tres","99.99",20);Sessao.iniciarCliente(cliente);
        var pix=confirmar(p,1,"pix",UUID.randomUUID());exigir(pix.subtotal().toPlainString().equals("99.99") && pix.desconto().toPlainString().equals("10.00") && pix.total().toPlainString().equals("89.99") && pix.pagamento().equals("confirmado"),"PIX incorreto.");
        exigir(texto("SELECT total::text FROM pedidos WHERE id="+pix.id()).equals("89.99") && texto("SELECT metodo FROM pagamentos WHERE id_pedido="+pix.id()).equals("pix"),"Pedido/pagamento não persistiram.");
        exigir(new EstoqueDAO().getQuantidade(p.getId())==19,"Estoque PIX.");ok("Compra PIX grava subtotal, desconto, total, pagamento e estoque juntos");

        var debito=confirmar(p,1,"debito",UUID.randomUUID());exigir(debito.total().toPlainString().equals("94.99"),"Débito incorreto.");var credito=confirmar(p,1,"credito",UUID.randomUUID());exigir(credito.total().toPlainString().equals("99.99"),"Crédito incorreto.");ok("Débito aplica 5%; crédito não inventa desconto ou cobrança real");

        UUID chave=UUID.randomUUID();var primeira=confirmar(p,1,"credito",chave);int saldo=new EstoqueDAO().getQuantidade(p.getId());long pedidos=numero("SELECT COUNT(*) FROM pedidos");
        var repetida=confirmar(p,1,"pix",chave);exigir(primeira.id()==repetida.id() && repetida.metodo().equals("credito") && new EstoqueDAO().getQuantidade(p.getId())==saldo && numero("SELECT COUNT(*) FROM pedidos")==pedidos,"Tentativa repetida duplicou/alterou compra.");ok("Repetir a chave da compra retorna o pedido original sem baixar estoque de novo");

        UUID simultanea=UUID.randomUUID();ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch inicio=new CountDownLatch(1);
        try{
            Future<Integer> a=executor.submit(()->{inicio.await();return confirmar(p,1,"pix",simultanea).id();});Future<Integer> b=executor.submit(()->{inicio.await();return confirmar(p,1,"pix",simultanea).id();});inicio.countDown();
            exigir(a.get(30,TimeUnit.SECONDS).equals(b.get(30,TimeUnit.SECONDS)),"Confirmação simultânea duplicada.");exigir(new EstoqueDAO().getQuantidade(p.getId())==saldo-1,"Baixa simultânea duplicada.");
        }finally{executor.shutdownNow();}ok("Duas confirmações simultâneas da mesma tentativa produzem um pedido");

        ItemPedido desatualizado=item(p,1);Sessao.iniciar(admin);p.setPreco(new BigDecimal("100.00"));new ProdutoDAO().atualizar(p);Sessao.iniciarCliente(cliente);
        long antes=numero("SELECT COUNT(*) FROM pedidos");int estoqueAntes=new EstoqueDAO().getQuantidade(p.getId());
        negar(()->compras.confirmar(cliente.getId(),List.of(desatualizado),"pix","Cliente Entrega","11999990000","Rua Teste, 100",UUID.randomUUID()),IllegalArgumentException.class);
        exigir(numero("SELECT COUNT(*) FROM pedidos")==antes && new EstoqueDAO().getQuantidade(p.getId())==estoqueAntes,"Preço alterado deixou compra parcial.");ok("Preço alterado exige revisão antes da compra, sem cobrança de novo valor silenciosa");

        Sessao.iniciar(admin);new ProdutoDAO().excluir(p.getId());Sessao.iniciarCliente(cliente);negar(()->confirmar(p,1,"pix",UUID.randomUUID()),IllegalArgumentException.class);
        Sessao.iniciar(admin);new ProdutoDAO().atualizar(p);Sessao.iniciarCliente(cliente);negar(()->confirmar(p,100,"pix",UUID.randomUUID()),IllegalArgumentException.class);
        negar(()->compras.confirmar(cliente.getId(),List.of(item(p,1)),"pix","Cliente Entrega","","",UUID.randomUUID()),IllegalArgumentException.class);ok("Produto inativo, falta de estoque e entrega incompleta recusados");

        Produto ultimo=produto("Disputa Parte Tres","12.34",1);Sessao.iniciarCliente(cliente);ExecutorService disputa=Executors.newFixedThreadPool(2);CountDownLatch largada=new CountDownLatch(1);
        try{
            Callable<Boolean> comprar=()->{largada.await();try{confirmar(ultimo,1,"credito",UUID.randomUUID());return true;}catch(IllegalArgumentException e){return false;}};
            Future<Boolean> a=disputa.submit(comprar),b=disputa.submit(comprar);largada.countDown();exigir(a.get(30,TimeUnit.SECONDS)^b.get(30,TimeUnit.SECONDS),"Disputa não produziu um vencedor.");exigir(new EstoqueDAO().getQuantidade(ultimo.getId())==0,"Estoque negativo/duplicado.");
        }finally{disputa.shutdownNow();}ok("Compras diferentes disputam a última unidade com apenas um vencedor");

        var boleto=confirmar(p,1,"boleto",UUID.randomUUID());exigir(boleto.pagamento().equals("pendente") && texto("SELECT status FROM pedidos WHERE id="+boleto.id()).equals("aberto"),"Boleto confirmado automaticamente.");
        negar(()->compras.confirmarPagamento(boleto.id()),SQLException.class);Sessao.iniciar(new UsuarioDAO().autenticar("vendedor","vendedor123"));negar(()->compras.confirmarPagamento(boleto.id()),SQLException.class);
        Sessao.iniciar(admin);int saldoBoleto=new EstoqueDAO().getQuantidade(p.getId());compras.confirmarPagamento(boleto.id());compras.confirmarPagamento(boleto.id());
        exigir(texto("SELECT status FROM pagamentos WHERE id_pedido="+boleto.id()).equals("confirmado") && numero("SELECT confirmado_por FROM pagamentos WHERE id_pedido="+boleto.id())==admin.getId() && new EstoqueDAO().getQuantidade(p.getId())==saldoBoleto,"Confirmação altera estoque ou perde responsável.");ok("Boleto pendente só é confirmado por admin e confirmação repetida não muda estoque");

        Sessao.iniciarCliente(cliente);String detalhe=compras.detalhes(pix.id());exigir(detalhe.contains("89,99") && detalhe.contains("Rua Teste, 100") && detalhe.contains("SIMULADO"),"Detalhes incompletos.");
        String erro=new ClienteLojaDAO().cadastrar("Outro Comprador",null,"outrocompra@teste.local",null,null,"senha12345");exigir(erro==null,"Outro cliente inválido.");Sessao.iniciarCliente(new ClienteLojaDAO().autenticar("outrocompra@teste.local","senha12345"));negar(()->compras.detalhes(pix.id()),IllegalArgumentException.class);ok("Detalhes preservam valores/entrega e não expõem pedido de outro cliente");

        Sessao.iniciarCliente(cliente);long pagamentosAntes=numero("SELECT COUNT(*) FROM pagamentos"),movAntes=numero("SELECT COUNT(*) FROM movimentos_estoque");int saldoFalha=new EstoqueDAO().getQuantidade(p.getId());
        sql("CREATE FUNCTION teste_pagamento_falha() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'falha de pagamento proposital'; END $$");sql("CREATE TRIGGER teste_pagamento BEFORE INSERT ON pagamentos FOR EACH ROW EXECUTE FUNCTION teste_pagamento_falha()");
        try{negar(()->confirmar(p,1,"pix",UUID.randomUUID()),SQLException.class);exigir(numero("SELECT COUNT(*) FROM pagamentos")==pagamentosAntes && numero("SELECT COUNT(*) FROM movimentos_estoque")==movAntes && new EstoqueDAO().getQuantidade(p.getId())==saldoFalha,"Pagamento falho deixou baixa/movimento parcial.");}
        finally{sql("DROP TRIGGER teste_pagamento ON pagamentos");sql("DROP FUNCTION teste_pagamento_falha()");}ok("Falha após baixar estoque desfaz pedido, itens, movimento e pagamento");

        Carrinho cart=new Carrinho();cart.adicionar(p,2,10);cart.itens().get(0).setQuantidade(99);exigir(cart.itens().get(0).getQuantidade()==2,"Janela altera carrinho por fora.");
        UUID reserva=cart.iniciarCheckout();negar(()->cart.iniciarCheckout(),IllegalArgumentException.class);negar(()->cart.remover(p.getId()),IllegalArgumentException.class);negar(()->cart.adicionar(p,1,10),IllegalArgumentException.class);
        cart.liberar(reserva);exigir(cart.iniciarCheckout().equals(reserva),"Retry perde chave idempotente.");cart.concluir(reserva);exigir(cart.itens().isEmpty() && !cart.bloqueado(),"Compra confirmada não limpa carrinho.");cart.adicionar(p,1,10);cart.encerrar();exigir(cart.itens().isEmpty(),"Logout não limpa carrinho.");ok("Carrinho entrega cópias, bloqueia múltiplos checkouts, preserva chave de retry e limpa no sucesso/logout");

        Pedido viaAntiga=new Pedido();viaAntiga.setIdCliente(cliente.getId());viaAntiga.adicionarItem(item(p,1));negar(()->new PedidoDAO().salvar(viaAntiga),SQLException.class);ok("Cliente não burla checkout pela gravação administrativa antiga");
        Sessao.iniciar(admin);String dia=texto("SELECT CURRENT_DATE::text");String total=texto("SELECT SUM(total)::text FROM pedidos WHERE status='finalizado' AND DATE(data_pedido)=CURRENT_DATE");
        var relatorio=new PedidoDAO().relatorioVendasPorPeriodo(dia,dia);exigir(new BigDecimal(relatorio.get(0)[2].replace(",",".")).compareTo(new BigDecimal(total))==0,"Relatório ignora descontos/pendências.");ok("Faturamento por período usa total final das compras confirmadas");
        System.out.println("APROVADO: "+cenarios+" cenários de compra e pagamento simulado em PostgreSQL real.");
    }
    private static Produto produto(String nome,String preco,int qtd)throws Exception{Sessao.iniciar(admin);Produto p=new Produto();p.setNome(nome);p.setPreco(new BigDecimal(preco));p.setIdCategoria(categoria);p.setAtivo(true);p.setEstoqueInicial(qtd);new ProdutoDAO().salvar(p);return p;}
    private static ItemPedido item(Produto p,int qtd){ItemPedido i=new ItemPedido();i.setIdProduto(p.getId());i.setNomeProduto(p.getNome());i.setQuantidade(qtd);i.setPrecoUnitario(p.getPrecoDecimal());return i;}
    private static CompraDAO.Resultado confirmar(Produto p,int qtd,String metodo,UUID chave)throws SQLException{return compras.confirmar(cliente.getId(),List.of(item(p,qtd)),metodo,"Cliente Entrega","11999990000","Rua Teste, 100",chave);}
    private static void sql(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement()){p.execute(s);}}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){r.next();return r.getString(1);}}
    private static long numero(String s)throws SQLException{return Long.parseLong(texto(s));}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void ok(String s){cenarios++;System.out.println("OK "+cenarios+": "+s);}
    private interface Operacao{void executar()throws Exception;}
    private static void negar(Operacao op,Class<? extends Throwable> tipo)throws Exception{try{op.executar();}catch(Throwable e){if(tipo.isInstance(e))return;throw new AssertionError("Erro diferente do esperado.",e);}throw new AssertionError("Operação indevida aceita.");}
}
