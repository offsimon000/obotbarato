import dao.*;
import model.*;
import util.*;
import view.loja.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Aciona componentes do próprio aplicativo no EDT, sem automação nativa ou janelas visíveis. */
public final class FluxoCheckout {
    private static TelaLoja loja;
    private static TelaCarrinho carrinho;
    private static TelaCheckout checkout;
    private static Cliente cliente;
    private static Produto produto;
    private static UUID chave;
    public static void main(String[] args)throws Exception{
        Thread vigia=new Thread(()->{try{Thread.sleep(90000);}catch(InterruptedException e){return;}System.err.println("Falha: fluxo de checkout excedeu limite.");System.exit(1);});vigia.setDaemon(true);vigia.start();
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){r.next();exigir(r.getString(1).equals("obotbarato_test"),"Base isolada obrigatória.");}
        Sessao.iniciar(new UsuarioDAO().autenticar("admin","admin123"));produto=new Produto();produto.setNome("Produto Fluxo UI");produto.setPreco(new BigDecimal("99.99"));produto.setAtivo(true);produto.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());produto.setEstoqueInicial(4);new ProdutoDAO().salvar(produto);
        cliente=new ClienteLojaDAO().autenticar("cliente@demo.local","cliente123");Sessao.iniciarCliente(cliente);
        SwingUtilities.invokeAndWait(()->{
            loja=new TelaLoja(cliente);preparar(loja);loja.getModeloCarrinho().adicionar(produto,2,4);
            carrinho=new TelaCarrinho(loja,cliente,loja.getCarrinho());preparar(carrinho);
            chave=loja.getModeloCarrinho().iniciarCheckout();checkout=new TelaCheckout(carrinho,cliente,loja.getCarrinho(),loja,chave);preparar(checkout);
            imagem(checkout,"checkout-resumo.png");botao(checkout,"Ir para pagamento >").doClick();
            radio(checkout,"PIX").doClick();imagem(checkout,"checkout-pagamento.png");
            JButton confirmar=botao(checkout,"Confirmar compra simulada");confirmar.doClick();confirmar.doClick();
        });
        aguardarConfirmacao();
        try(Connection c=ConexaoBanco.getConexao();PreparedStatement p=c.prepareStatement("SELECT p.total,pg.metodo,pg.status,(SELECT COUNT(*) FROM pedidos WHERE chave_compra=?) AS contagem FROM pedidos p JOIN pagamentos pg ON pg.id_pedido=p.id WHERE chave_compra=?")){
            p.setObject(1,chave);p.setObject(2,chave);try(ResultSet r=p.executeQuery()){exigir(r.next() && r.getBigDecimal(1).toPlainString().equals("179.98") && r.getString(2).equals("pix") && r.getString(3).equals("confirmado") && r.getInt(4)==1,"Checkout não gravou desconto/pagamento único.");}
        }
        exigir(new EstoqueDAO().getQuantidade(produto.getId())==2,"Baixa de checkout duplicada.");
        SwingUtilities.invokeAndWait(()->{imagem(checkout,"checkout-confirmacao.png");checkout.dispatchEvent(new WindowEvent(checkout,WindowEvent.WINDOW_CLOSING));exigir(loja.getCarrinho().isEmpty(),"Fechar confirmação preservou carrinho comprado.");
            loja.getModeloCarrinho().adicionar(produto,1,2);chave=loja.getModeloCarrinho().iniciarCheckout();checkout=new TelaCheckout(carrinho,cliente,loja.getCarrinho(),loja,chave);preparar(checkout);checkout.dispose();exigir(!loja.getModeloCarrinho().bloqueado() && loja.getCarrinho().size()==1,"Fechar antes de comprar perdeu itens ou bloqueio.");
            chave=loja.getModeloCarrinho().iniciarCheckout();checkout=new TelaCheckout(carrinho,cliente,loja.getCarrinho(),loja,chave);preparar(checkout);botao(checkout,"Ir para pagamento >").doClick();botao(checkout,"Confirmar compra simulada").doClick();
        });
        aguardarConfirmacao();
        SwingUtilities.invokeAndWait(()->{botao(checkout,"Continuar comprando").doClick();exigir(loja.getCarrinho().isEmpty() && !checkout.isDisplayable() && !carrinho.isDisplayable(),"Continuar comprando não encerra o checkout.");for(Window w:Window.getWindows())w.dispose();Sessao.encerrar();});
        System.out.println("APROVADO: fluxo Swing de resumo → PIX → confirmação, duplo clique, fechamento antes/depois e continuar comprando; carrinho e estoque consistentes.");System.exit(0);
    }
    private static void aguardarConfirmacao()throws Exception{
        CountDownLatch pronto=new CountDownLatch(1);
        SwingUtilities.invokeAndWait(()->{Timer timer=new Timer(100,null);timer.addActionListener(e->{if(loja.getCarrinho().isEmpty() && encontrar(checkout,JButton.class,"Continuar comprando",false)!=null){timer.stop();pronto.countDown();}});timer.start();});
        exigir(pronto.await(45,TimeUnit.SECONDS),"Confirmação não chegou.");
    }
    private static void preparar(JFrame frame){frame.addNotify();frame.validate();}
    private static JButton botao(Container c,String texto){JButton b=encontrar(c,JButton.class,texto,false);if(b==null)throw new AssertionError("Botão ausente: "+texto);return b;}
    private static JRadioButton radio(Container c,String prefixo){JRadioButton b=encontrar(c,JRadioButton.class,prefixo,true);if(b==null)throw new AssertionError("Método ausente.");return b;}
    private static <T extends AbstractButton>T encontrar(Container c,Class<T> tipo,String texto,boolean prefixo){for(Component filho:c.getComponents()){if(tipo.isInstance(filho)){T b=tipo.cast(filho);if(prefixo?b.getText().startsWith(texto):b.getText().equals(texto))return b;}if(filho instanceof Container){T b=encontrar((Container)filho,tipo,texto,prefixo);if(b!=null)return b;}}return null;}
    private static void imagem(JFrame frame,String nome){try{frame.validate();Container p=frame.getContentPane();BufferedImage img=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();try{p.printAll(g);}finally{g.dispose();}Path pasta=Path.of("docs/validacao-ui");Files.createDirectories(pasta);ImageIO.write(img,"png",pasta.resolve(nome).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private static void exigir(boolean b,String texto){if(!b)throw new AssertionError(texto);}
}
