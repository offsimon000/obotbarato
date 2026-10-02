import dao.*;
import model.*;
import util.*;
import view.TelaGestaoPedidos;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;

/** Aciona botões e workers reais; fornece respostas aos diálogos sem exibir janelas. */
public final class FluxoGestaoPedidos {
    private static class TelaTeste extends TelaGestaoPedidos {
        TelaTeste(Cliente cliente){super(null,cliente);}
        protected boolean confirmarAvanco(GestaoPedidoDAO.Resumo p,String destino){return true;}
        protected String solicitarMotivo(GestaoPedidoDAO.Resumo p){return "Desistência registrada pelo teste da interface";}
        protected void erro(Throwable e){throw new AssertionError("Erro inesperado na interface.",e);}
    }
    private static TelaTeste tela;
    private static int pedido;
    public static void main(String[] args)throws Exception{
        Thread limite=new Thread(()->{try{Thread.sleep(90000);}catch(InterruptedException e){return;}System.err.println("Falha: gestão Swing excedeu limite.");System.exit(1);});limite.setDaemon(true);limite.start();
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){r.next();exigir(r.getString(1).equals("obotbarato_test"),"Base isolada obrigatória.");}
        Usuario admin=new UsuarioDAO().autenticar("admin","admin123");Sessao.iniciar(admin);
        Produto p=new Produto();p.setNome("Produto Gestao UI");p.setPreco(new BigDecimal("20.00"));p.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());p.setAtivo(true);p.setEstoqueInicial(5);new ProdutoDAO().salvar(p);
        String email="gestaoui"+UUID.randomUUID().toString().substring(0,8)+"@teste.local";
        exigir(new ClienteLojaDAO().cadastrar("Cliente Gestao Interface",null,email,null,null,"senha12345")==null,"Cliente não cadastrado.");
        Cliente cliente=new ClienteLojaDAO().autenticar(email,"senha12345");Sessao.iniciarCliente(cliente);
        pedido=compra(cliente,p);Sessao.iniciar(admin);
        SwingUtilities.invokeAndWait(()->{tela=new TelaTeste(null);preparar();campo("filtro-cliente").setText(email);botao("Filtrar / Atualizar").doClick();});
        aguardar(()->tabela().getRowCount()==1 && tabela().getValueAt(0,0).equals(pedido));
        SwingUtilities.invokeAndWait(()->{exigir(tabela().getValueAt(0,4).equals("Aberto"),"Compra paga não está aberta.");tabela().setRowSelectionInterval(0,0);botao("Avançar entrega").doClick();botao("Avançar entrega").doClick();});
        aguardar(()->botao("Avançar entrega").isEnabled() && tabela().getValueAt(0,4).equals("Em preparo"));
        SwingUtilities.invokeAndWait(()->{imagem("gestao-pedidos.png");tabela().setRowSelectionInterval(0,0);botao("Cancelar pedido").doClick();botao("Cancelar pedido").doClick();});
        aguardar(()->botao("Cancelar pedido").isEnabled() && tabela().getValueAt(0,4).equals("Cancelado"));
        exigir(new EstoqueDAO().getQuantidade(p.getId())==5,"Cancelamento duplicou/perdeu saldo.");
        SwingUtilities.invokeAndWait(()->tela.dispose());Sessao.iniciarCliente(cliente);pedido=compra(cliente,p);
        SwingUtilities.invokeAndWait(()->{tela=new TelaTeste(cliente);preparar();});
        aguardar(()->tabela().getRowCount()==2);
        SwingUtilities.invokeAndWait(()->{exigir(!campo("filtro-cliente").isEnabled(),"Cliente pode consultar titulares alheios.");exigir(encontrar(tela,JButton.class,"Avançar entrega")==null && encontrar(tela,JButton.class,"Nova venda no balcão")==null,"Cliente recebeu comando da equipe.");int row=linhaPedido();tabela().setRowSelectionInterval(row,row);imagem("meus-pedidos.png");botao("Cancelar pedido").doClick();});
        aguardar(()->botao("Cancelar pedido").isEnabled() && tabela().getValueAt(linhaPedido(),4).equals("Cancelado"));
        exigir(new EstoqueDAO().getQuantidade(p.getId())==5,"Cliente não recebeu devolução.");
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();Sessao.encerrar();});
        System.out.println("APROVADO: fluxo Swing de filtro → avanço → cancelamento pela equipe, duplo clique e cancelamento pessoal; confirmação/motivo fornecidos pelo teste, sem janelas visíveis.");System.exit(0);
    }
    private static int compra(Cliente cliente,Produto p)throws SQLException{ItemPedido i=new ItemPedido();i.setIdProduto(p.getId());i.setQuantidade(1);i.setPrecoUnitario(p.getPrecoDecimal());return new CompraDAO().confirmar(cliente.getId(),java.util.List.of(i),"pix",cliente.getNome(),"11999990000","Rua Interface, 100",UUID.randomUUID()).id();}
    private static int linhaPedido(){for(int i=0;i<tabela().getRowCount();i++)if(tabela().getValueAt(i,0).equals(pedido))return i;throw new AssertionError("Pedido ausente.");}
    private static void preparar(){tela.addNotify();tela.validate();}
    private static void aguardar(BooleanSupplier condicao)throws Exception{CountDownLatch pronto=new CountDownLatch(1);SwingUtilities.invokeAndWait(()->{Timer timer=new Timer(100,null);timer.addActionListener(e->{if(condicao.getAsBoolean()){timer.stop();pronto.countDown();}});timer.start();});exigir(pronto.await(30,TimeUnit.SECONDS),"Atualização da interface não chegou.");}
    private static JTable tabela(){return encontrar(tela,JTable.class,null);}
    private static JTextField campo(String nome){return encontrar(tela,JTextField.class,nome);}
    private static JButton botao(String texto){JButton b=encontrar(tela,JButton.class,texto);if(b==null)throw new AssertionError("Botão ausente: "+texto);return b;}
    private static <T extends Component>T encontrar(Container c,Class<T> tipo,String nome){for(Component filho:c.getComponents()){if(tipo.isInstance(filho) && (nome==null || nome.equals(filho.getName()) || filho instanceof AbstractButton && nome.equals(((AbstractButton)filho).getText())))return tipo.cast(filho);if(filho instanceof Container){T b=encontrar((Container)filho,tipo,nome);if(b!=null)return b;}}return null;}
    private static void layout(Container c){c.doLayout();for(Component filho:c.getComponents())if(filho instanceof Container)layout((Container)filho);}
    private static void imagem(String nome){try{Container p=tela.getContentPane();layout(p);BufferedImage img=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();try{p.printAll(g);}finally{g.dispose();}ImageIO.write(img,"png",Path.of("docs/validacao-ui",nome).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
}
