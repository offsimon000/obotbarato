import dao.*;
import model.*;
import util.*;
import view.*;
import view.loja.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;

/** Máscaras reais, validação de datas e fluxo do cliente, sem janelas visíveis. */
public final class InterfaceBrasileira {
    private static class ClientesTeste extends TelaClientes {
        ClientesTeste(){super(null);}
        JPanel formulario(JTextField... campos){return criarFormulario(campos);}
    }
    private static class LojaTeste extends TelaLoja {
        LojaTeste(Cliente cliente){super(cliente);}
        protected void mostrarJanela(JFrame janela){janela.addNotify();janela.validate();}
    }
    private static LojaTeste loja;
    private static TelaCarrinho cart;
    public static void main(String[] args)throws Exception{
        Thread limite=new Thread(()->{try{Thread.sleep(90000);}catch(InterruptedException e){return;}System.err.println("Falha: interface brasileira excedeu limite.");System.exit(1);});limite.setDaemon(true);limite.start();
        exigir(Datas.ler("29/02/2024").equals(LocalDate.of(2024,2,29)),"Data bissexta inválida.");
        exigir(Datas.opcional("")==null,"Filtro vazio não é opcional.");
        for(String s:new String[]{"29/02/2025","31/04/2026","09/10/26","2026-10-09","00/10/2026","09/10/0000"}){
            try{Datas.ler(s);throw new AssertionError("Data indevida aceita: "+s);}catch(IllegalArgumentException esperado){}
        }
        SwingUtilities.invokeAndWait(()->{
            try{
                JTextField data=new JTextField();MascaraCampo.aplicarData(data);
                for(char digito:"09102026".toCharArray())data.getDocument().insertString(data.getCaretPosition(),String.valueOf(digito),null);
                exigir(data.getText().equals("09/10/2026"),"Digitação sem barras falhou.");
                data.select(3,5);data.replaceSelection("11");exigir(data.getText().equals("09/11/2026"),"Edição do mês falhou.");
                data.selectAll();data.replaceSelection("");exigir(data.getText().isEmpty(),"Não é possível limpar a data.");
                data.setText("29022024");exigir(Datas.ler(data.getText()).getDayOfMonth()==29,"Colagem sem barras falhou.");
            }catch(Exception e){throw new RuntimeException(e);}
        });
        System.out.println("OK: datas brasileiras estritas, anos bissextos, máscara por digitação/colagem, edição no meio e limpeza.");
        try(Connection c=ConexaoBanco.getConexao();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){r.next();exigir(r.getString(1).equals("obotbarato_test"),"Base isolada obrigatória.");}
        Sessao.iniciar(new UsuarioDAO().autenticar("admin","admin123"));
        SwingUtilities.invokeAndWait(()->{
            ClientesTeste tela=new ClientesTeste();
            JTextField[] campos={Tema.campo(22),Tema.campo(14),Tema.campo(22),Tema.campo(15),Tema.campo(30)};
            campos[0].setText("Cliente de exemplo");campos[2].setText("cliente@exemplo.local");
            JPanel form=tela.formulario(campos);form.setSize(form.getPreferredSize());layout(form);
            for(JTextField campo:campos)exigir(campo.getWidth()>=400 && campo.getHeight()>=28,"Campo voltou a ficar estreito.");imagem(form,"formulario-cliente.png");tela.dispose();
            TelaRelatorios rel=new TelaRelatorios(null);preparar(rel);
            JTextField inicio=encontrar(rel,JTextField.class,"relatorio-inicio"),fim=encontrar(rel,JTextField.class,"relatorio-fim");
            inicio.setText("01102026");fim.setText("09102026");exigir(inicio.getText().equals("01/10/2026") && fim.getText().equals("09/10/2026"),"Relatório não usa máscara brasileira.");imagem(rel.getContentPane(),"relatorios.png");rel.dispose();
        });
        Cliente cliente=new ClienteLojaDAO().autenticar("cliente@demo.local","cliente123");Sessao.iniciarCliente(cliente);
        Produto produto=new ProdutoDAO().listarCatalogo("").stream().filter(p->p.quantidade()>1).findFirst().orElseThrow().produto();
        SwingUtilities.invokeAndWait(()->{loja=new LojaTeste(cliente);preparar(loja);});
        aguardar(()->encontrar(loja,JButton.class,"Adicionar ao carrinho")!=null);
        SwingUtilities.invokeAndWait(()->{
            exigir(encontrar(loja,JLabel.class,"*")==null,"Asterisco permaneceu na marca.");
            loja.abrirCarrinho();loja.abrirCarrinho();
            exigir(janelas(TelaCarrinho.class)==1,"Dois carrinhos abertos para a mesma sessão.");cart=janela(TelaCarrinho.class);
            exigir(!botao(cart,"Finalizar Compra").isEnabled() && !botao(cart,"Limpar Carrinho").isEnabled(),"Carrinho vazio possui ação indevida.");
            loja.adicionarAoCarrinho(produto);
        });
        aguardar(()->loja.getCarrinho().size()==1);
        SwingUtilities.invokeAndWait(()->{
            exigir(botao(cart,"Finalizar Compra").isEnabled(),"Carrinho preenchido não pode comprar.");
            java.util.UUID chave=loja.getModeloCarrinho().iniciarCheckout();loja.atualizarBadgeCarrinho();
            exigir(!botao(cart,"Finalizar Compra").isEnabled() && !botao(cart,"Limpar Carrinho").isEnabled(),"Carrinho reservado pode ser alterado.");
            loja.getModeloCarrinho().liberar(chave);loja.atualizarBadgeCarrinho();
            exigir(botao(cart,"Finalizar Compra").isEnabled(),"Fechar pagamento deixou carrinho bloqueado.");
            imagem(loja.getContentPane(),"loja.png");imagem(cart.getContentPane(),"carrinho.png");
            loja.abrirPedidos();loja.abrirPedidos();exigir(janelas(TelaMeusPedidos.class)==1,"Abriu dois históricos pessoais.");
        });
        TelaMeusPedidos pedidos=janela(TelaMeusPedidos.class);
        aguardar(()->botao(pedidos,"Filtrar / Atualizar").isEnabled() && encontrar(pedidos,JLabel.class,"Atualizando pedidos...")==null);
        SwingUtilities.invokeAndWait(()->{
            JTextField inicio=encontrar(pedidos,JTextField.class,"filtro-inicio");inicio.setText("01012000");exigir(inicio.getText().equals("01/01/2000"),"Filtro pessoal não usa máscara.");
            botao(pedidos,"Filtrar / Atualizar").doClick();
        });
        aguardar(()->botao(pedidos,"Filtrar / Atualizar").isEnabled() && encontrar(pedidos,JLabel.class,"Atualizando pedidos...")==null);
        SwingUtilities.invokeAndWait(()->{imagem(pedidos.getContentPane(),"meus-pedidos.png");for(Window w:Window.getWindows())w.dispose();Sessao.encerrar();});
        System.out.println("APROVADO: formulário com campos largos, relatórios/filtros mascarados, loja sem asterisco, catálogo carregado, janelas únicas e ações corretas para carrinho vazio/reservado/liberado.");System.exit(0);
    }
    private static void preparar(JFrame f){f.addNotify();f.validate();}
    private static void aguardar(BooleanSupplier condicao)throws Exception{CountDownLatch pronto=new CountDownLatch(1);SwingUtilities.invokeAndWait(()->{Timer timer=new Timer(100,null);timer.addActionListener(e->{if(condicao.getAsBoolean()){timer.stop();pronto.countDown();}});timer.start();});exigir(pronto.await(30,TimeUnit.SECONDS),"Atualização não chegou.");}
    private static int janelas(Class<? extends Window> tipo){int qtd=0;for(Window w:Window.getWindows())if(tipo.isInstance(w) && w.isDisplayable())qtd++;return qtd;}
    private static <T extends Window>T janela(Class<T> tipo){for(Window w:Window.getWindows())if(tipo.isInstance(w) && w.isDisplayable())return tipo.cast(w);throw new AssertionError("Janela ausente.");}
    private static JButton botao(Container c,String texto){JButton b=encontrar(c,JButton.class,texto);if(b==null)throw new AssertionError("Botão ausente: "+texto);return b;}
    private static <T extends Component>T encontrar(Container c,Class<T> tipo,String nome){for(Component filho:c.getComponents()){if(tipo.isInstance(filho) && (nome==null || nome.equals(filho.getName()) || filho instanceof AbstractButton && nome.equals(((AbstractButton)filho).getText()) || filho instanceof JLabel && nome.equals(((JLabel)filho).getText())))return tipo.cast(filho);if(filho instanceof Container){T r=encontrar((Container)filho,tipo,nome);if(r!=null)return r;}}return null;}
    private static void layout(Container c){c.doLayout();for(Component filho:c.getComponents())if(filho instanceof Container)layout((Container)filho);}
    private static void imagem(Container c,String nome){try{layout(c);BufferedImage img=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=img.createGraphics();try{c.printAll(g);}finally{g.dispose();}Path pasta=Path.of("docs/validacao-ui");Files.createDirectories(pasta);ImageIO.write(img,"png",pasta.resolve(nome).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
}
