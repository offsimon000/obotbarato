import dao.*;
import model.*;
import util.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;

public final class CadastrosEAcesso {
    private static int cenarios;
    private static Usuario admin,vendedor;
    private static Cliente cliente;
    public static void main(String[] args)throws Exception{
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");
        admin=new UsuarioDAO().autenticar("admin","admin123");vendedor=new UsuarioDAO().autenticar("vendedor","vendedor123");cliente=new ClienteLojaDAO().autenticar("cliente@demo.local","cliente123");
        UsuarioDAO usuarios=new UsuarioDAO();ProdutoDAO produtos=new ProdutoDAO();EstoqueDAO estoque=new EstoqueDAO();ClienteDAO clientes=new ClienteDAO();CategoriaDAO categorias=new CategoriaDAO();
        Sessao.encerrar();negar(()->clientes.listarTodos(),SQLException.class);negar(()->usuarios.listar(),SQLException.class);ok("Operações administrativas negadas sem sessão");
        Sessao.iniciar(vendedor);exigir(!Sessao.isAdminAtivo(),"Vendedor confundido com admin.");clientes.listarTodos();produtos.listarManutencao("");
        negar(()->usuarios.listar(),SQLException.class);negar(()->categorias.salvar(0,"Negada",true),SQLException.class);negar(()->estoque.entrada(1,1),SQLException.class);negar(()->clientes.excluir(cliente.getId()),SQLException.class);negar(()->new CodigoAcessoDAO().emitir(cliente.getId()),SQLException.class);ok("Vendedor consulta e atende, mas não gerencia usuários, categorias, estoque, desativação ou senhas");
        Sessao.iniciarCliente(cliente);negar(()->clientes.listarTodos(),SQLException.class);negar(()->new PedidoDAO().listarTodos(),SQLException.class);negar(()->new PedidoDAO().listarPorCliente(cliente.getId()+100000),SQLException.class);negar(()->new ClienteLojaDAO().adicionarFavorito(cliente.getId()+100000,1),SQLException.class);new PedidoDAO().listarPorCliente(cliente.getId());ok("Cliente só consulta seu histórico e altera seus favoritos");

        Sessao.iniciar(admin);Usuario u=new Usuario(0,"Equipe Teste","equipe_teste","vendedor");u.setSenha("senha12345");usuarios.salvar(u);
        exigir(u.getId()>0 && usuarios.autenticar(" EQUIPE_TESTE ","senha12345")!=null,"Criação/normalização falhou.");
        u.setNome("Equipe Alterada");u.setSenha("");usuarios.atualizar(u);exigir(usuarios.autenticar("equipe_teste","senha12345")!=null,"Senha vazia alterou senha.");
        Usuario duplicado=new Usuario(0,"Duplicado Teste","EQUIPE_TESTE","admin");duplicado.setSenha("senha12345");negar(()->usuarios.salvar(duplicado),SQLException.class);
        u.setAtivo(false);usuarios.atualizar(u);exigir(usuarios.autenticar("equipe_teste","senha12345")==null,"Inativo entrou.");
        Sessao.iniciar(u);negar(()->clientes.listarTodos(),SQLException.class);Sessao.iniciar(admin);ok("Usuários: criar, editar sem trocar senha, duplicidade, desativação e revogação da sessão");

        Usuario unico=new Usuario(admin.getId(),admin.getNome(),"admin","admin");unico.setAtivo(false);negar(()->usuarios.atualizar(unico),IllegalArgumentException.class);
        unico.setAtivo(true);unico.setPerfil("vendedor");negar(()->usuarios.atualizar(unico),IllegalArgumentException.class);
        exigir(usuarios.autenticar("admin","admin123")!=null,"Último admin perdido.");ok("Último administrador não pode ser desativado ou rebaixado");

        String md5=HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest("curta".getBytes(StandardCharsets.UTF_8)));
        sql("UPDATE usuarios SET ativo=true,senha='"+md5+"' WHERE id="+u.getId());
        exigir(usuarios.autenticar("equipe_teste","curta")!=null && texto("SELECT senha FROM usuarios WHERE id="+u.getId()).startsWith("pbkdf2$600000$"),"MD5 não migrado.");
        byte[] salt=new byte[16];Arrays.fill(salt,(byte)3);MessageDigest sha=MessageDigest.getInstance("SHA-256");sha.update(salt);
        String antigo=Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(sha.digest("senha12345".getBytes(StandardCharsets.UTF_8)));
        sql("UPDATE usuarios SET senha='"+antigo+"' WHERE id="+u.getId());exigir(usuarios.autenticar("equipe_teste","senha12345")!=null,"SHA256 antigo recusado.");
        exigir(texto("SELECT senha FROM usuarios WHERE id="+u.getId()).startsWith("pbkdf2$600000$"),"SHA não migrado.");
        exigir(!SenhaCripto.verificar("x","pbkdf2$abc$xxx$xxx") && !SenhaCripto.verificar("x","inválido"),"Hash corrompido aceito.");negar(()->SenhaCripto.hashear("curta"),IllegalArgumentException.class);ok("PBKDF2 e migração transparente de MD5/SHA256 antigos, inclusive senha antiga curta");

        Cliente novo=new Cliente(0,"Cliente Acesso",null," NOVO@teste.local ",null,null);clientes.salvar(novo);exigir(new ClienteLojaDAO().autenticar("novo@teste.local","senha12345")==null,"Cliente sem senha entrou.");
        CodigoAcessoDAO acesso=new CodigoAcessoDAO();String token=acesso.emitir(novo.getId());exigir(!texto("SELECT codigo_hash FROM acessos_cliente WHERE id_cliente="+novo.getId()).equals(token),"Código puro guardado.");
        exigir(!acesso.redefinir("novo@teste.local","00000000000000000000000000000000","senha12345"),"Código errado aceito.");
        exigir(acesso.redefinir("novo@teste.local",token,"senha12345"),"Ativação falhou.");exigir(!acesso.redefinir("novo@teste.local",token,"outrasenha123"),"Código reutilizado.");
        exigir(new ClienteLojaDAO().autenticar("novo@teste.local","senha12345")!=null,"Senha nova não autentica.");ok("Ativação de cliente administrativo com código secreto de uso único");

        token=acesso.emitir(novo.getId());sql("UPDATE acessos_cliente SET expira_em=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE id_cliente="+novo.getId());exigir(!acesso.redefinir("novo@teste.local",token,"outrasenha123"),"Expirado aceito.");
        token=acesso.emitir(novo.getId());for(int i=0;i<5;i++)exigir(!acesso.redefinir("novo@teste.local","00000000000000000000000000000000","outrasenha123"),"Código errado aceito.");exigir(!acesso.redefinir("novo@teste.local",token,"outrasenha123"),"Tentativas não bloquearam.");
        token=acesso.emitir(novo.getId());exigir(acesso.redefinir("novo@teste.local",token,"outrasenha123"),"Reemissão não recupera.");
        exigir(new ClienteLojaDAO().autenticar("novo@teste.local","senha12345")==null && new ClienteLojaDAO().autenticar("novo@teste.local","outrasenha123")!=null,"Senha antiga não invalidada.");ok("Recuperação: expiração, bloqueio após cinco erros, reemissão e substituição de senha");

        negar(()->Validacao.cpf("111.111.111-11"),IllegalArgumentException.class);negar(()->Validacao.cpf("52998224724"),IllegalArgumentException.class);exigir(Validacao.cpf("52998224725").equals("529.982.247-25"),"CPF normalização.");
        negar(()->Validacao.email("x@"),IllegalArgumentException.class);negar(()->Validacao.telefone("123"),IllegalArgumentException.class);exigir(Validacao.telefone("1133334444").equals("(11) 3333-4444"),"Fixo inválido.");
        String erro=new ClienteLojaDAO().cadastrar("Cliente CPF","11111111111","cpf@teste.local","","","senha12345");exigir(erro!=null,"CPF inválido cadastrado.");
        Cliente duplo=new Cliente(0,"Cliente Duplo","52998224725","outro@teste.local",null,null);negar(()->clientes.salvar(duplo),SQLException.class);ok("Validação compartilhada: CPF, email, fixo/celular, duplicidade com máscara diferente");

        SwingUtilities.invokeAndWait(()->{try{
            JTextField c=new JTextField();MascaraCampo.aplicarCpf(c);c.setText("52998224725");
            ((AbstractDocument)c.getDocument()).replace(0,3,"123",null);exigir(c.getText().equals("123.982.247-25"),"Seleção substituída na posição errada.");
            c.selectAll();c.replaceSelection("");exigir(c.getText().isEmpty(),"Limpeza falhou.");
            JTextField t=new JTextField();MascaraCampo.aplicarTelefone(t);t.setText("1133334444");exigir(t.getText().equals("(11) 3333-4444"),"Máscara fixo.");t.setText("11999990000");exigir(t.getText().equals("(11) 99999-0000"),"Máscara celular.");
        }catch(Exception e){throw new RuntimeException(e);}});ok("Máscaras: edição no meio, substituição, limpeza, fixo e celular no EDT");

        categorias.salvar(0,"Categoria Parte Dois",true);Categoria cat=categorias.listar(true).stream().filter(c->c.nome().equals("Categoria Parte Dois")).findFirst().orElseThrow();
        negar(()->categorias.salvar(0,"categoria parte dois",true),SQLException.class);
        Produto p=new Produto();p.setNome("Produto Parte Dois");p.setIdCategoria(cat.id());p.setPreco(new java.math.BigDecimal("12.34"));p.setAtivo(true);p.setEstoqueInicial(7);p.setEstoqueMinimo(4);
        BufferedImage png=new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(png,"png",out);p.setImagem(out.toByteArray());produtos.salvar(p);
        exigir(estoque.getQuantidade(p.getId())==7 && estoque.getMinimo(p.getId())==4 && Arrays.equals(produtos.buscarPorId(p.getId()).getImagem(),p.getImagem()),"Cadastro incompleto.");
        negar(()->categorias.salvar(cat.id(),cat.nome(),false),IllegalArgumentException.class);ok("Produto com imagem, saldo inicial e mínimo; categoria em uso protegida");

        estoque.entrada(p.getId(),3,"Reposição teste");estoque.saida(p.getId(),2,"Amostras teste");estoque.definirMinimo(p.getId(),9);
        exigir(estoque.getQuantidade(p.getId())==8 && estoque.getMinimo(p.getId())==9,"Saldo/mínimo incorretos.");exigir(estoque.historico(p.getId()).size()==3,"Movimento ausente.");
        long antes=numero("SELECT COUNT(*) FROM movimentos_estoque WHERE id_produto="+p.getId());negar(()->estoque.saida(p.getId(),100),SQLException.class);exigir(estoque.getQuantidade(p.getId())==8 && numero("SELECT COUNT(*) FROM movimentos_estoque WHERE id_produto="+p.getId())==antes,"Falha alterou saldo/histórico.");
        exigir(estoque.listarEstoqueBaixo().stream().anyMatch(r->r[0].equals(p.getNome())),"Alerta não usa mínimo real.");ok("Movimentações auditadas, mínimo real e rollback quando falta estoque");

        sql("CREATE FUNCTION teste_falha_movimento() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'falha proposital movimento'; END $$");
        sql("CREATE TRIGGER teste_movimento BEFORE INSERT ON movimentos_estoque FOR EACH ROW EXECUTE FUNCTION teste_falha_movimento()");
        try{negar(()->estoque.entrada(p.getId(),3),SQLException.class);exigir(estoque.getQuantidade(p.getId())==8,"Saldo confirmado sem histórico.");}
        finally{sql("DROP TRIGGER teste_movimento ON movimentos_estoque");sql("DROP FUNCTION teste_falha_movimento()");}
        ok("Falha ao gravar movimento desfaz a alteração de estoque");

        produtos.excluir(p.getId());categorias.salvar(cat.id(),cat.nome(),false);exigir(produtos.listarTodos().stream().noneMatch(x->x.getId()==p.getId()),"Inativo no catálogo.");p.setAtivo(true);negar(()->produtos.atualizar(p),IllegalArgumentException.class);categorias.salvar(cat.id(),cat.nome(),true);produtos.atualizar(p);
        exigir(produtos.listarTodos().stream().anyMatch(x->x.getId()==p.getId()),"Reativação falhou.");ok("Desativação/reativação de produto e categoria preservam dados e catálogo");

        Cliente autenticado=new ClienteLojaDAO().autenticar("novo@teste.local","outrasenha123");clientes.excluir(novo.getId());exigir(new ClienteLojaDAO().autenticar("novo@teste.local","outrasenha123")==null,"Cliente inativo entrou.");Sessao.iniciarCliente(autenticado);negar(()->new PedidoDAO().listarPorCliente(novo.getId()),SQLException.class);Sessao.iniciar(admin);clientes.reativar(novo.getId());exigir(new ClienteLojaDAO().autenticar("novo@teste.local","outrasenha123")!=null,"Cliente não reativado.");Sessao.encerrar();exigir(Sessao.getIdUsuario()==0 && Sessao.getIdCliente()==0,"Logout mantém identidade.");ok("Cliente desativado perde acesso; reativação mantém senha; logout limpa identidade");
        System.out.println("APROVADO: "+cenarios+" cenários da parte 2 em PostgreSQL real.");
    }
    private static void sql(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement()){p.execute(s);}}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){r.next();return r.getString(1);}}
    private static long numero(String s)throws SQLException{return Long.parseLong(texto(s));}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void ok(String s){cenarios++;System.out.println("OK "+cenarios+": "+s);}
    private interface Operacao{void executar()throws Exception;}
    private static void negar(Operacao op,Class<? extends Throwable> tipo)throws Exception{try{op.executar();}catch(Throwable e){if(tipo.isInstance(e))return;throw new AssertionError("Erro diferente do esperado.",e);}throw new AssertionError("Operação indevida foi aceita.");}
}
