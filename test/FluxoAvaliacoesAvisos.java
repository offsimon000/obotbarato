import dao.*;import model.*;import util.*;import view.*;import view.loja.*;
import javax.swing.*;import java.awt.*;import java.awt.image.BufferedImage;import java.nio.file.*;import java.sql.*;import java.math.BigDecimal;import java.util.UUID;import java.util.concurrent.*;import java.util.function.BooleanSupplier;import javax.imageio.ImageIO;

/** Aciona avaliação e leitura de avisos por componentes Swing reais, sem janelas visíveis. */
public final class FluxoAvaliacoesAvisos {
    public static void main(String[] args)throws Exception{
        Thread vigia=new Thread(()->{try{Thread.sleep(120000);}catch(InterruptedException e){return;}System.err.println("Falha: fluxo avaliação/avisos excedeu limite.");System.exit(1);});vigia.setDaemon(true);vigia.start();
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");Usuario admin=new UsuarioDAO().autenticar("admin","admin123");Sessao.iniciar(admin);
        Produto p=new Produto();p.setNome("Produto Avaliacao Interface");p.setPreco(new BigDecimal("20.00"));p.setEstoqueInicial(10);p.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());p.setAtivo(true);new ProdutoDAO().salvar(p);
        String email="avalui"+UUID.randomUUID().toString().substring(0,8)+"@teste.local";ClienteLojaDAO d=new ClienteLojaDAO();exigir(d.cadastrar("Cliente Aval UI",null,email,"11999990000","Rua UI, 100","senha12345")==null,"Cadastro falhou.");Cliente cliente=d.autenticar(email,"senha12345");Sessao.iniciarCliente(cliente);
        ItemPedido item=new ItemPedido();item.setIdProduto(p.getId());item.setQuantidade(1);item.setPrecoUnitario(p.getPrecoDecimal());int pedido=new CompraDAO().confirmar(cliente.getId(),java.util.List.of(item),"pix",cliente.getNome(),cliente.getTelefone(),cliente.getEndereco(),UUID.randomUUID()).id();
        Sessao.iniciar(admin);GestaoPedidoDAO pedidos=new GestaoPedidoDAO();pedidos.avancar(pedido,"aberto","em_preparo");pedidos.avancar(pedido,"em_preparo","enviado");pedidos.avancar(pedido,"enviado","entregue");new EstoqueDAO().atualizar(p.getId(),4);Sessao.iniciarCliente(cliente);
        final TelaAvaliacao[] tela=new TelaAvaliacao[1];final TelaNotificacoes[] avisos=new TelaNotificacoes[1];final TelaLoja[] loja=new TelaLoja[1];
        SwingUtilities.invokeAndWait(()->{loja[0]=new TelaLoja(cliente);preparar(loja[0]);tela[0]=new TelaAvaliacao(loja[0],pedido);preparar(tela[0]);});aguardar(()->botao(tela[0],"Salvar avaliação").isEnabled());
        SwingUtilities.invokeAndWait(()->{encontrar(tela[0],JSpinner.class,"avaliacao-nota").setValue(4);encontrar(tela[0],JTextArea.class,"avaliacao-comentario").setText("Produto bem apresentado e compra fácil de acompanhar.");botao(tela[0],"Salvar avaliação").doClick();botao(tela[0],"Salvar avaliação").doClick();});aguardar(()->temTexto(tela[0],"Avaliação salva."));
        exigir(texto("SELECT COUNT(*) FROM avaliacoes WHERE id_cliente="+cliente.getId()+" AND id_produto="+p.getId()).equals("1"),"Duplo clique duplicou avaliação.");
        SwingUtilities.invokeAndWait(()->{imagem(tela[0],"avaliacao-cliente.png");loja[0].atualizarCatalogo();avisos[0]=new TelaNotificacoes(loja[0]);preparar(avisos[0]);});aguardar(()->tabela(avisos[0]).getRowCount()==4);
        SwingUtilities.invokeAndWait(()->{tabela(avisos[0]).setRowSelectionInterval(0,0);imagem(avisos[0],"notificacoes-cliente.png");botao(avisos[0],"Marcar como lida").doClick();});aguardar(()->tabela(avisos[0]).getRowCount()==3);
        exigir(new NotificacaoDAO().listar(true).size()==3 && new ProdutoDAO().buscarParaCarrinho(p.getId()).media().compareTo(new BigDecimal("4"))==0,"Leitura ou média incorreta.");
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});Sessao.iniciar(admin);SwingUtilities.invokeAndWait(()->{avisos[0]=new TelaNotificacoes(null);preparar(avisos[0]);});aguardar(()->temTexto(avisos[0],"avisos."));
        SwingUtilities.invokeAndWait(()->{JTable t=tabela(avisos[0]);for(int i=0;i<t.getRowCount();i++)if(t.getValueAt(i,1).equals("estoque")){t.setRowSelectionInterval(i,i);break;}imagem(avisos[0],"notificacoes-equipe.png");for(Window w:Window.getWindows())w.dispose();Sessao.encerrar();});
        System.out.println("APROVADO: fluxo Swing de avaliar pedido entregue, confirmar sem duplicar, carregar média, consultar avisos próprios/marcar leitura e consultar alertas da equipe.");System.exit(0);
    }
    private static void preparar(JFrame f){f.addNotify();f.validate();}
    private static void aguardar(BooleanSupplier b)throws Exception{CountDownLatch l=new CountDownLatch(1);SwingUtilities.invokeAndWait(()->{Timer t=new Timer(100,null);t.addActionListener(e->{if(b.getAsBoolean()){t.stop();l.countDown();}});t.start();});exigir(l.await(35,TimeUnit.SECONDS),"Interface não concluiu operação.");}
    private static JTable tabela(Container c){return encontrar(c,JTable.class,null);}private static JButton botao(Container c,String nome){JButton b=encontrar(c,JButton.class,nome);if(b==null)throw new AssertionError("Botão ausente: "+nome);return b;}
    private static <T extends Component>T encontrar(Container c,Class<T> tipo,String nome){for(Component f:c.getComponents()){if(tipo.isInstance(f) && (nome==null || nome.equals(f.getName()) || f instanceof AbstractButton && nome.equals(((AbstractButton)f).getText())))return tipo.cast(f);if(f instanceof Container){T b=encontrar((Container)f,tipo,nome);if(b!=null)return b;}}return null;}
    private static boolean temTexto(Container c,String trecho){for(Component f:c.getComponents()){if(f instanceof JLabel && ((JLabel)f).getText().contains(trecho))return true;if(f instanceof Container && temTexto((Container)f,trecho))return true;}return false;}
    private static void layout(Container c){c.doLayout();for(Component f:c.getComponents())if(f instanceof Container)layout((Container)f);}
    private static void imagem(JFrame f,String nome){try{Container p=f.getContentPane();layout(p);BufferedImage img=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();try{p.printAll(g);}finally{g.dispose();}ImageIO.write(img,"png",Path.of("docs/validacao-ui",nome).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){r.next();return r.getString(1);}}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
}
