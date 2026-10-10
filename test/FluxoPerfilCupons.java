import dao.*;import model.*;import util.*;import view.*;import view.loja.*;
import javax.swing.*;import java.awt.*;import java.awt.image.BufferedImage;import java.nio.file.*;import java.sql.*;import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;import java.util.concurrent.*;import java.util.function.BooleanSupplier;import javax.imageio.ImageIO;

/** Exercita os botões e Workers de perfil e checkout com cupom, sem janelas visíveis. */
public final class FluxoPerfilCupons {
    private static TelaLoja loja;private static TelaPerfil perfil;private static TelaCheckout checkout;
    public static void main(String[] args)throws Exception{
        Thread vigia=new Thread(()->{try{Thread.sleep(120000);}catch(InterruptedException e){return;}System.err.println("Falha: perfil/cupom Swing excedeu limite.");System.exit(1);});vigia.setDaemon(true);vigia.start();
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");
        Usuario admin=new UsuarioDAO().autenticar("admin","admin123");Sessao.iniciar(admin);
        LocalDate hoje=LocalDate.parse(texto("SELECT CURRENT_DATE::text"));String codigo="GUI10_"+UUID.randomUUID().toString().substring(0,8).toUpperCase();new CupomDAO().salvar(0,codigo,new BigDecimal("10"),hoje,hoje.plusDays(1),true);
        Produto produto=new Produto();produto.setNome("Produto Cupom Interface");produto.setPreco(new BigDecimal("100.00"));produto.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());produto.setAtivo(true);produto.setEstoqueInicial(4);new ProdutoDAO().salvar(produto);
        String email="perfilui"+UUID.randomUUID().toString().substring(0,8)+"@teste.local";ClienteLojaDAO dao=new ClienteLojaDAO();exigir(dao.cadastrar("Perfil Interface",null,email,"11999990000","Rua UI, 100","senha12345")==null,"Cliente não cadastrado.");Cliente cliente=dao.autenticar(email,"senha12345");Sessao.iniciarCliente(cliente);
        SwingUtilities.invokeAndWait(()->{loja=new TelaLoja(cliente);preparar(loja);perfil=new TelaPerfil(loja,cliente);preparar(perfil);});
        aguardar(()->botao(perfil,"Salvar perfil").isEnabled());
        SwingUtilities.invokeAndWait(()->{campo(perfil,"perfil-nome").setText("Mariana Interface");campo(perfil,"perfil-endereco").setText("Rua UI Atualizada, 200");campo(perfil,"perfil-atual").setText("senha12345");campo(perfil,"perfil-nova").setText("perfilNova123");campo(perfil,"perfil-confirmacao").setText("perfilNova123");botao(perfil,"Salvar perfil").doClick();botao(perfil,"Salvar perfil").doClick();});
        aguardar(()->temTexto(perfil,"Perfil salvo."));
        exigir(dao.autenticar(email,"senha12345")==null && dao.autenticar(email,"perfilNova123")!=null && cliente.getNome().equals("Mariana Interface"),"Perfil não atualizou acesso/sessão.");
        SwingUtilities.invokeAndWait(()->{imagem(perfil,"perfil-cliente.png");loja.getModeloCarrinho().adicionar(produto,1,4);TelaCarrinho cart=new TelaCarrinho(loja,cliente,loja.getCarrinho());preparar(cart);UUID chave=loja.getModeloCarrinho().iniciarCheckout();checkout=new TelaCheckout(cart,cliente,loja.getCarrinho(),loja,chave);preparar(checkout);botao(checkout,"Ir para pagamento >").doClick();campo(checkout,"checkout-cupom").setText(codigo);exigir(!botao(checkout,"Confirmar compra simulada").isEnabled(),"Cupom não validado permite confirmar.");botao(checkout,"Aplicar cupom").doClick();});
        aguardar(()->botao(checkout,"Confirmar compra simulada").isEnabled() && !campo(checkout,"checkout-cupom").isEditable());
        SwingUtilities.invokeAndWait(()->{botao(checkout,"Remover").doClick();exigir(campo(checkout,"checkout-cupom").getText().isEmpty() && campo(checkout,"checkout-cupom").isEditable(),"Remover não libera cupom.");campo(checkout,"checkout-cupom").setText(codigo);botao(checkout,"Aplicar cupom").doClick();});
        aguardar(()->botao(checkout,"Confirmar compra simulada").isEnabled() && !campo(checkout,"checkout-cupom").isEditable());
        SwingUtilities.invokeAndWait(()->{radio(checkout,"PIX").doClick();exigir(temTexto(checkout,"81,00"),"PIX/cupom não mostra total correto.");imagem(checkout,"checkout-cupom.png");botao(checkout,"Confirmar compra simulada").doClick();botao(checkout,"Confirmar compra simulada").doClick();});
        aguardar(()->encontrar(checkout,JButton.class,"Continuar comprando")!=null && loja.getCarrinho().isEmpty());
        exigir(texto("SELECT total::text||'/'||desconto_cupom::text||'/'||desconto_pagamento::text||'/'||endereco_entrega FROM pedidos WHERE id_cliente="+cliente.getId()+" ORDER BY id DESC LIMIT 1").equals("81.00/10.00/9.00/Rua UI Atualizada, 200"),"Valores ou entrega do perfil incorretos.");
        exigir(texto("SELECT COUNT(*) FROM pedidos WHERE id_cliente="+cliente.getId()).equals("1") && new EstoqueDAO().getQuantidade(produto.getId())==3,"Duplo clique duplicou compra.");
        SwingUtilities.invokeAndWait(()->{imagem(checkout,"checkout-cupom-confirmacao.png");for(Window w:Window.getWindows())w.dispose();});Sessao.iniciar(admin);
        final TelaCupons[] tela=new TelaCupons[1];SwingUtilities.invokeAndWait(()->{tela[0]=new TelaCupons(null);preparar(tela[0]);});aguardar(()->temValor(tela[0],codigo));
        SwingUtilities.invokeAndWait(()->{imagem(tela[0],"cupons-admin.png");for(Window w:Window.getWindows())w.dispose();Sessao.encerrar();});
        System.out.println("APROVADO: fluxo Swing de salvar perfil/trocar senha, aplicar/remover/reaplicar cupom, PIX R$81, confirmação única e entrega com dados atualizados; painel de cupons carregado.");System.exit(0);
    }
    private static void preparar(JFrame f){f.addNotify();f.validate();}
    private static void aguardar(BooleanSupplier b)throws Exception{CountDownLatch l=new CountDownLatch(1);SwingUtilities.invokeAndWait(()->{Timer t=new Timer(100,null);t.addActionListener(e->{if(b.getAsBoolean()){t.stop();l.countDown();}});t.start();});exigir(l.await(35,TimeUnit.SECONDS),"Interface não concluiu operação.");}
    private static JTextField campo(Container c,String nome){return encontrar(c,JTextField.class,nome);}
    private static JButton botao(Container c,String nome){JButton b=encontrar(c,JButton.class,nome);if(b==null)throw new AssertionError("Botão ausente: "+nome);return b;}
    private static JRadioButton radio(Container c,String prefixo){for(Component f:c.getComponents()){if(f instanceof JRadioButton && ((JRadioButton)f).getText().startsWith(prefixo))return (JRadioButton)f;if(f instanceof Container){JRadioButton b=radio((Container)f,prefixo);if(b!=null)return b;}}return null;}
    private static <T extends Component>T encontrar(Container c,Class<T> tipo,String nome){for(Component f:c.getComponents()){if(tipo.isInstance(f) && (nome.equals(f.getName()) || f instanceof AbstractButton && nome.equals(((AbstractButton)f).getText())))return tipo.cast(f);if(f instanceof Container){T b=encontrar((Container)f,tipo,nome);if(b!=null)return b;}}return null;}
    private static boolean temTexto(Container c,String trecho){for(Component f:c.getComponents()){if(f instanceof JLabel && ((JLabel)f).getText().contains(trecho))return true;if(f instanceof Container && temTexto((Container)f,trecho))return true;}return false;}
    private static boolean temValor(Container c,String valor){for(Component f:c.getComponents()){if(f instanceof JTable){JTable t=(JTable)f;for(int i=0;i<t.getRowCount();i++)if(t.getValueAt(i,0).equals(valor))return true;}if(f instanceof Container && temValor((Container)f,valor))return true;}return false;}
    private static void layout(Container c){c.doLayout();for(Component f:c.getComponents())if(f instanceof Container)layout((Container)f);}
    private static void imagem(JFrame f,String nome){try{Container p=f.getContentPane();layout(p);BufferedImage img=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();try{p.printAll(g);}finally{g.dispose();}ImageIO.write(img,"png",Path.of("docs/validacao-ui",nome).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){r.next();return r.getString(1);}}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
}
