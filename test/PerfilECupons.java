import dao.*;import model.*;import util.*;
import java.math.BigDecimal;import java.sql.*;import java.time.LocalDate;import java.util.*;import java.util.concurrent.*;

/** Regras de perfil e descontos verificadas em PostgreSQL isolado. */
public final class PerfilECupons {
    private static Usuario admin;private static Cliente cliente,outro;private static Produto produto;private static LocalDate hoje;private static int cenarios;
    private static final ClienteLojaDAO perfis=new ClienteLojaDAO();private static final CupomDAO cupons=new CupomDAO();private static final CompraDAO compras=new CompraDAO();
    public static void main(String[] args)throws Exception{
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");hoje=LocalDate.parse(texto("SELECT CURRENT_DATE::text"));
        admin=new UsuarioDAO().autenticar("admin","admin123");Sessao.iniciar(admin);
        produto=new Produto();produto.setNome("Produto Perfil Cupom");produto.setPreco(new BigDecimal("100.00"));produto.setAtivo(true);produto.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());produto.setEstoqueInicial(80);new ProdutoDAO().salvar(produto);
        cliente=cliente("Perfil Cupom","perfilcupom@teste.local");outro=cliente("Outro Perfil","outroperfil@teste.local");
        Sessao.encerrar();negar(()->perfis.buscarPerfil(cliente.getId()),SQLException.class);negar(()->cupons.listar(),SQLException.class);
        Sessao.iniciarCliente(outro);negar(()->perfis.buscarPerfil(cliente.getId()),SQLException.class);negar(()->perfis.atualizarPerfil(copia(cliente),"senha12345",null),SQLException.class);ok("Perfil privado e operações sem sessão ou de outro titular recusadas");

        Sessao.iniciarCliente(cliente);Cliente dados=copia(cliente);dados.setNome("Perfil Atualizado");dados.setEmail(" PERFIL.NOVO@TESTE.LOCAL ");dados.setTelefone("11988887777");dados.setEndereco("Rua Perfil, 200");
        negar(()->perfis.atualizarPerfil(dados,"errada",null),IllegalArgumentException.class);exigir(perfis.buscarPerfil(cliente.getId()).getNome().equals("Perfil Cupom"),"Senha incorreta alterou dados.");
        cliente=perfis.atualizarPerfil(dados,"senha12345",null);exigir(cliente.getEmail().equals("perfil.novo@teste.local") && perfis.autenticar("perfilcupom@teste.local","senha12345")==null && perfis.autenticar(cliente.getEmail(),"senha12345")!=null,"Perfil/login não atualizado.");ok("Dados/e-mail normalizados só mudam com senha atual correta; senha vazia preserva acesso");

        String hash=texto("SELECT senha FROM clientes WHERE id="+cliente.getId());Cliente duplicado=copia(cliente);duplicado.setEmail(outro.getEmail());negar(()->perfis.atualizarPerfil(duplicado,"senha12345","novaSenha123"),SQLException.class);
        exigir(texto("SELECT senha FROM clientes WHERE id="+cliente.getId()).equals(hash) && perfis.buscarPerfil(cliente.getId()).getEmail().equals(cliente.getEmail()),"Duplicidade deixou mudança parcial de senha.");Cliente invalido=copia(cliente);invalido.setCpf("11111111111");negar(()->perfis.atualizarPerfil(invalido,"senha12345",null),IllegalArgumentException.class);ok("E-mail duplicado/CPF inválido recusados sem trocar a senha parcialmente");

        Sessao.iniciar(admin);String codigo=new CodigoAcessoDAO().emitir(cliente.getId());Sessao.iniciarCliente(cliente);
        cliente=perfis.atualizarPerfil(copia(cliente),"senha12345","novaSenha123");exigir(perfis.autenticar(cliente.getEmail(),"senha12345")==null && perfis.autenticar(cliente.getEmail(),"novaSenha123")!=null,"Senha antiga continua válida.");
        exigir(!new CodigoAcessoDAO().redefinir(cliente.getEmail(),codigo,"outraSenha123") && texto("SELECT senha FROM clientes WHERE id="+cliente.getId()).startsWith("pbkdf2$"),"Código anterior reviveu ou hash fraco.");ok("Troca de senha mantém PBKDF2 e invalida código temporário anterior");

        int antigo=compra(null,null,UUID.randomUUID()).id();Cliente novo=copia(cliente);novo.setEndereco("Rua Nova, 300");cliente=perfis.atualizarPerfil(novo,"novaSenha123",null);exigir(texto("SELECT endereco_entrega FROM pedidos WHERE id="+antigo).equals("Rua Entrega, 100"),"Perfil alterou entrega de pedido antigo.");ok("Editar endereço cadastral não muda snapshot de entrega de compra anterior");

        Sessao.iniciar(new UsuarioDAO().autenticar("vendedor","vendedor123"));negar(()->cupons.salvar(0,"TESTE10",new BigDecimal("10"),hoje,hoje,true),SQLException.class);Sessao.iniciarCliente(cliente);negar(()->cupons.listar(),SQLException.class);
        Sessao.iniciar(admin);cupons.salvar(0," teste10 ",new BigDecimal("10"),hoje.minusDays(1),hoje.plusDays(1),true);int cupom=cupons.listar().stream().filter(c->c.codigo().equals("TESTE10")).findFirst().orElseThrow().id();
        negar(()->cupons.salvar(0,"TESTE10",new BigDecimal("20"),hoje,hoje,true),SQLException.class);negar(()->cupons.salvar(0,"ZERO",BigDecimal.ZERO,hoje,hoje,true),IllegalArgumentException.class);negar(()->cupons.salvar(0,"MAIS100",new BigDecimal("100.01"),hoje,hoje,true),IllegalArgumentException.class);negar(()->cupons.salvar(0,"DATAS",new BigDecimal("10"),hoje,hoje.minusDays(1),true),IllegalArgumentException.class);ok("Somente admin gerencia cupons; código único, percentual e datas validados");

        cupons.salvar(0,"EXPIRADO",new BigDecimal("10"),hoje.minusDays(2),hoje.minusDays(1),true);cupons.salvar(0,"FUTURO",new BigDecimal("10"),hoje.plusDays(1),hoje.plusDays(2),true);cupons.salvar(0,"INATIVO",new BigDecimal("10"),hoje,hoje,false);cupons.salvar(0,"HOJE",new BigDecimal("10"),hoje,hoje,true);
        Sessao.iniciarCliente(cliente);exigir(cupons.consultar(" hoje ").percentual().compareTo(new BigDecimal("10"))==0,"Validade não inclui o último dia.");for(String s:new String[]{"EXPIRADO","FUTURO","INATIVO","INEXISTENTE"})negar(()->cupons.consultar(s),IllegalArgumentException.class);ok("Consulta normalizada aceita o dia inicial/final e rejeita futuro, expirado ou inativo");

        var valores=DescontosCompra.calcular(new BigDecimal("100"),"pix",new BigDecimal("10"));exigir(valores.cupom().toPlainString().equals("10.00") && valores.pagamento().toPlainString().equals("9.00") && valores.total().toPlainString().equals("81.00"),"Combinação calculada sobre base errada.");
        var centavos=DescontosCompra.calcular(new BigDecimal("99.99"),"debito",new BigDecimal("10"));exigir(centavos.total().toPlainString().equals("85.49"),"Arredondamento combinado incorreto.");ok("Cupom antes do pagamento: R$100/10%/PIX = R$81; centavos arredondados por etapa");

        UUID chave=UUID.randomUUID();var compra=compra("TESTE10",new BigDecimal("10"),chave);
        exigir(compra.total().toPlainString().equals("81.00") && compra.descontoCupom().toPlainString().equals("10.00") && compra.descontoPagamento().toPlainString().equals("9.00"),"Valores não persistiram.");
        exigir(texto("SELECT codigo_cupom||'/'||percentual_cupom::text||'/'||desconto::text FROM pedidos WHERE id="+compra.id()).equals("TESTE10/10.00/19.00"),"Snapshot incompleto.");int saldo=new EstoqueDAO().getQuantidade(produto.getId());long qt=numero("SELECT COUNT(*) FROM pedidos");
        Sessao.iniciar(admin);cupons.salvar(cupom,"RENOMEADO",new BigDecimal("30"),hoje,hoje,false);Sessao.iniciarCliente(cliente);var retry=compra("TESTE10",new BigDecimal("10"),chave);exigir(retry.id()==compra.id() && retry.total().toPlainString().equals("81.00") && new EstoqueDAO().getQuantidade(produto.getId())==saldo,"Retry recalculou ou duplicou.");exigir(compras.detalhes(compra.id()).contains("TESTE10"),"Edição do cupom perdeu histórico.");ok("Pedido guarda código/percentual/descontos; edição posterior e retry não recalculam compra");

        Sessao.iniciar(admin);cupons.salvar(cupom,"TESTE10",new BigDecimal("20"),hoje,hoje,true);Sessao.iniciarCliente(cliente);negar(()->compra("TESTE10",new BigDecimal("10"),UUID.randomUUID()),IllegalArgumentException.class);exigir(numero("SELECT COUNT(*) FROM pedidos")==qt && new EstoqueDAO().getQuantidade(produto.getId())==saldo,"Percentual alterado deixou compra parcial.");negar(()->compra("EXPIRADO",new BigDecimal("10"),UUID.randomUUID()),IllegalArgumentException.class);ok("Cupom mudado/expirado na confirmação exige revisão sem pedido ou baixa parcial");

        Sessao.iniciar(admin);cupons.salvar(cupom,"TESTE10",new BigDecimal("10"),hoje,hoje,true);Sessao.iniciarCliente(cliente);UUID simultanea=UUID.randomUUID();ExecutorService ex=Executors.newFixedThreadPool(2);CountDownLatch inicio=new CountDownLatch(1);
        try{Future<Integer>a=ex.submit(()->{inicio.await();return compra("TESTE10",new BigDecimal("10"),simultanea).id();}),b=ex.submit(()->{inicio.await();return compra("TESTE10",new BigDecimal("10"),simultanea).id();});inicio.countDown();exigir(a.get(30,TimeUnit.SECONDS).equals(b.get(30,TimeUnit.SECONDS)),"Concorrência duplicou cupom/compra.");}finally{ex.shutdownNow();}ok("Duas confirmações simultâneas com cupom geram um único pedido");

        new GestaoPedidoDAO().cancelar(compra.id(),"Cancelamento com cupom");exigir(texto("SELECT desconto_cupom::text||'/'||desconto_pagamento::text FROM pedidos WHERE id="+compra.id()).equals("10.00/9.00"),"Cancelamento perdeu snapshot.");ok("Cancelamento preserva os descontos históricos e devolve estoque pelo fluxo existente");

        long antes=numero("SELECT COUNT(*) FROM pedidos"),mov=numero("SELECT COUNT(*) FROM movimentos_estoque");int saldoAntes=new EstoqueDAO().getQuantidade(produto.getId());sql("CREATE FUNCTION teste_cupom_falha() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'falha proposital do pagamento'; END $$");sql("CREATE TRIGGER teste_cupom BEFORE INSERT ON pagamentos FOR EACH ROW EXECUTE FUNCTION teste_cupom_falha()");
        try{negar(()->compra("TESTE10",new BigDecimal("10"),UUID.randomUUID()),SQLException.class);exigir(numero("SELECT COUNT(*) FROM pedidos")==antes && numero("SELECT COUNT(*) FROM movimentos_estoque")==mov && new EstoqueDAO().getQuantidade(produto.getId())==saldoAntes,"Cupom deixou operação parcial.");}finally{sql("DROP TRIGGER teste_cupom ON pagamentos");sql("DROP FUNCTION teste_cupom_falha()");}ok("Falha após gravar desconto/baixar estoque desfaz pedido, itens, movimento e pagamento");

        Sessao.iniciar(admin);cupons.salvar(0,"GRATIS",new BigDecimal("100"),hoje,hoje,true);Sessao.iniciarCliente(cliente);var gratis=compra("GRATIS",new BigDecimal("100"),UUID.randomUUID());exigir(gratis.total().signum()==0 && gratis.descontoPagamento().signum()==0,"Cupom 100% produziu total negativo.");
        negar(()->compra(null,new BigDecimal("10"),UUID.randomUUID()),IllegalArgumentException.class);ok("Cupom 100% mantém total zero e percentual sem código é recusado");
        System.out.println("APROVADO: "+cenarios+" cenários de perfil e cupons em PostgreSQL real.");
    }
    private static Cliente cliente(String nome,String email)throws Exception{exigir(perfis.cadastrar(nome,null,email,"11999990000","Rua Perfil, 100","senha12345")==null,"Cadastro falhou.");return perfis.autenticar(email,"senha12345");}
    private static Cliente copia(Cliente c){return new Cliente(c.getId(),c.getNome(),c.getCpf(),c.getEmail(),c.getTelefone(),c.getEndereco());}
    private static CompraDAO.Resultado compra(String codigo,BigDecimal percentual,UUID chave)throws SQLException{ItemPedido i=new ItemPedido();i.setIdProduto(produto.getId());i.setQuantidade(1);i.setPrecoUnitario(produto.getPrecoDecimal());return compras.confirmar(cliente.getId(),List.of(i),"pix","Cliente Entrega","11999990000","Rua Entrega, 100",chave,codigo,percentual);}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){r.next();return r.getString(1);}}
    private static long numero(String s)throws SQLException{return Long.parseLong(texto(s));}
    private static void sql(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement()){p.execute(s);}}
    @FunctionalInterface private interface Operacao{void executar()throws Exception;}
    private static void negar(Operacao op,Class<? extends Throwable> tipo)throws Exception{try{op.executar();}catch(Throwable e){if(tipo.isInstance(e))return;throw new AssertionError("Erro diferente do esperado.",e);}throw new AssertionError("Operação indevida aceita.");}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void ok(String s){cenarios++;System.out.println("OK "+cenarios+": "+s);}
}
