import dao.*;import model.*;import util.*;import view.*;
import javax.swing.*;import java.awt.*;import java.awt.event.*;import java.awt.image.BufferedImage;import java.nio.file.*;import java.math.BigDecimal;import java.lang.reflect.Field;import java.util.List;import java.util.concurrent.*;import java.util.function.BooleanSupplier;import javax.imageio.ImageIO;

/** Interações reais em Swing: filtros, consistência, exportação e retorno de janelas. */
public final class FluxoRelatoriosNavegacao {
    private static TelaDashboard dashboard;private static TelaRelatorios relatorios;
    private static final class Anterior extends JFrame {boolean retornou;public void setVisible(boolean b){retornou=b;}public void toFront(){} }
    public static void main(String[] args)throws Exception {
        Thread vigia=new Thread(()->{try{Thread.sleep(120000);}catch(InterruptedException e){return;}System.err.println("Falha: fluxo relatórios/navegação excedeu limite.");System.exit(1);});vigia.setDaemon(true);vigia.start();
        try(var c=ConexaoBanco.getConexao();var s=c.createStatement();var r=s.executeQuery("SELECT current_database()")){r.next();exigir(r.getString(1).equals("obotbarato_test"),"Base isolada obrigatória.");}
        Usuario admin=new UsuarioDAO().autenticar("admin","admin123");Sessao.iniciar(admin);
        SwingUtilities.invokeAndWait(()->{dashboard=new TelaDashboard(null);preparar(dashboard);relatorios=new TelaRelatorios(null);preparar(relatorios);});aguardar(()->botao(dashboard,"Atualizar").isEnabled() && botao(relatorios,"Atualizar").isEnabled());
        SwingUtilities.invokeAndWait(()->{exigir(encontrar(dashboard,JTabbedPane.class,null).getSelectedIndex()==0 && encontrar(relatorios,JTabbedPane.class,null).getSelectedIndex()==1,"Aba inicial incorreta.");periodo(dashboard,"01022031","02022031");periodo(relatorios,"01022031","02022031");});aguardar(()->botao(dashboard,"Atualizar").isEnabled() && botao(relatorios,"Atualizar").isEnabled());
        SwingUtilities.invokeAndWait(()->{try{
            exigir(temTexto(dashboard,"362,00") && temTexto(dashboard,"120,67"),"Indicadores não correspondem às vendas conhecidas.");JTable t=encontrar(relatorios,JTable.class,null);exigir(t.getRowCount()==2,"Vendas por dia incorretas.");BigDecimal total=BigDecimal.ZERO;List<Object[]> linhas=new java.util.ArrayList<>();List<String> colunas=new java.util.ArrayList<>();for(int c=0;c<t.getColumnCount();c++)colunas.add(t.getColumnName(c));for(int r=0;r<t.getRowCount();r++){total=total.add((BigDecimal)t.getValueAt(r,5));Object[] linha=new Object[t.getColumnCount()];for(int c=0;c<linha.length;c++)linha[c]=t.getValueAt(r,c);linhas.add(linha);}exigir(total.compareTo(new BigDecimal("362.00"))==0,"Tabela diverge do dashboard.");
            Path p=Files.createTempFile(Path.of(".local"),"csv-interface-",".csv");ExportarCsv.salvar(p,colunas,linhas);exigir(Files.readString(p).contains("\"162,00\""),"CSV diverge da interface.");Files.delete(p);
            imagem(dashboard,"dashboard.png");imagem(relatorios,"relatorios-vendas.png");
            JTabbedPane abas=encontrar(relatorios,JTabbedPane.class,null);abas.setSelectedIndex(2);exigir(botao(relatorios,"Exportar CSV").isEnabled(),"Ranking não exporta.");imagem(relatorios,"relatorios-ranking.png");abas.setSelectedIndex(3);imagem(relatorios,"relatorios-pedidos.png");abas.setSelectedIndex(4);imagem(relatorios,"relatorios-estoque.png");abas.setSelectedIndex(1);
            campo(relatorios,"relatorio-fim").setText("03022031");exigir(!botao(relatorios,"Exportar CSV").isEnabled() && temTexto(relatorios,"Filtro alterado"),"CSV permite exportar período divergente.");
            periodo(relatorios,"02022031","01022031");exigir(temTexto(relatorios,"posterior") && !botao(relatorios,"Exportar CSV").isEnabled(),"Período inválido aceito.");periodo(relatorios,"01012034","01012034");
        }catch(Exception e){throw new RuntimeException(e);}});aguardar(()->botao(relatorios,"Atualizar").isEnabled() && temTexto(relatorios,"0 pedidos"));
        SwingUtilities.invokeAndWait(()->{exigir(!botao(relatorios,"Exportar CSV").isEnabled() && temTexto(relatorios,"Nenhum registro"),"Aba vazia/exportação incorreta.");
            Anterior anterior=new Anterior();TelaNotificacoes n=new TelaNotificacoes(anterior);preparar(n);botao(n,"nav-voltar").doClick();exigir(!n.isDisplayable() && anterior.retornou,"Voltar não retorna à origem.");
            anterior.retornou=false;n=new TelaNotificacoes(anterior);preparar(n);n.dispatchEvent(new WindowEvent(n,WindowEvent.WINDOW_CLOSING));exigir(!n.isDisplayable() && anterior.retornou,"X difere de Voltar.");
            anterior.retornou=false;n=new TelaNotificacoes(anterior);preparar(n);campoPrivado(n,"gravando",true);botao(n,"nav-voltar").doClick();n.dispatchEvent(new WindowEvent(n,WindowEvent.WINDOW_CLOSING));exigir(n.isDisplayable() && !anterior.retornou,"Leitura em andamento permite saída.");campoPrivado(n,"gravando",false);botao(n,"nav-voltar").doClick();
            TelaGestaoPedidos g=new TelaGestaoPedidos(anterior);preparar(g);campoPrivado(g,"processando",true);botao(g,"nav-voltar").doClick();g.dispatchEvent(new WindowEvent(g,WindowEvent.WINDOW_CLOSING));exigir(g.isDisplayable(),"Pedido em gravação permite saída.");campoPrivado(g,"processando",false);g.dispose();
            anterior.retornou=false;n=new TelaNotificacoes(anterior);preparar(n);Sessao.encerrar();botao(n,"nav-voltar").doClick();exigir(!n.isDisplayable() && !anterior.retornou,"Logout ressuscitou janela de sessão anterior.");for(Window w:Window.getWindows())w.dispose();
        });
        System.out.println("APROVADO: fluxo Swing de dashboard/relatórios, filtros brasileiros, valores conhecidos, abas/CSV, estados vazios, Voltar/X, gravação protegida e sessão encerrada.");System.exit(0);
    }
    private static void campoPrivado(Object o,String nome,boolean v){try{Field f=o.getClass().getDeclaredField(nome);f.setAccessible(true);f.setBoolean(o,v);}catch(Exception e){throw new RuntimeException(e);}}
    private static void periodo(JFrame f,String de,String ate){campo(f,"relatorio-inicio").setText(de);campo(f,"relatorio-fim").setText(ate);botao(f,"Atualizar").doClick();}
    private static JTextField campo(Container c,String nome){return encontrar(c,JTextField.class,nome);}
    private static JButton botao(Container c,String nome){JButton b=encontrar(c,JButton.class,nome);if(b==null)throw new AssertionError("Botão ausente: "+nome);return b;}
    private static <T extends Component>T encontrar(Container c,Class<T> tipo,String nome){for(Component f:c.getComponents()){if(tipo.isInstance(f) && (nome==null || nome.equals(f.getName()) || f instanceof AbstractButton && nome.equals(((AbstractButton)f).getText())))return tipo.cast(f);if(f instanceof Container){T r=encontrar((Container)f,tipo,nome);if(r!=null)return r;}}return null;}
    private static boolean temTexto(Container c,String trecho){for(Component f:c.getComponents()){if(f instanceof JLabel && ((JLabel)f).getText().contains(trecho))return true;if(f instanceof Container && temTexto((Container)f,trecho))return true;}return false;}
    private static void aguardar(BooleanSupplier b)throws Exception{CountDownLatch l=new CountDownLatch(1);SwingUtilities.invokeAndWait(()->{Timer t=new Timer(100,null);t.addActionListener(e->{if(b.getAsBoolean()){t.stop();l.countDown();}});t.start();});exigir(l.await(35,TimeUnit.SECONDS),"Interface não concluiu a consulta.");}
    private static void preparar(JFrame f){f.addNotify();f.validate();}
    private static void layout(Container c){c.doLayout();for(Component f:c.getComponents())if(f instanceof Container)layout((Container)f);}
    private static void imagem(JFrame f,String nome)throws Exception{Container p=f.getContentPane();layout(p);BufferedImage i=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=i.createGraphics();try{p.printAll(g);}finally{g.dispose();}ImageIO.write(i,"png",Path.of("docs/validacao-ui",nome).toFile());}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
}
