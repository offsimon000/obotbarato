import dao.*;import util.*;import view.*;import java.sql.*;import java.nio.file.*;import java.util.*;import java.time.*;import java.awt.*;import java.awt.image.BufferedImage;import java.util.concurrent.*;import javax.swing.*;import javax.imageio.ImageIO;

/** Compara registros anteriores e saldo; renderiza as próprias telas sem mostrar janelas. */
public final class ConferirSimulacaoLocal {
    private static final String[] TABELAS={"usuarios","categorias","produtos","clientes","pedidos","pagamentos","itens_pedido","historico_pedidos","avaliacoes","notificacoes","leituras_notificacoes"};
    public static void main(String[] args)throws Exception {
        if(args.length!=2 || !Set.of("antes","depois").contains(args[0]))throw new IllegalArgumentException("Uso: antes/depois arquivo-snapshot");
        Path arquivo=Path.of(args[1]);Properties dados=new Properties();try(Connection c=ConexaoBanco.getConexao()){
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT current_database()")){r.next();if(!r.getString(1).equals("obotbarato"))throw new AssertionError("Base local obrigatória.");}
            if(args[0].equals("antes")){
                for(String t:TABELAS)try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT "+(t.equals("pagamentos")?"id_pedido":"id")+",md5(row(x.*)::text) FROM "+t+" x")){while(r.next())dados.setProperty(t+":"+r.getString(1),r.getString(2));}
                try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id_produto,quantidade FROM estoque")){while(r.next())dados.setProperty("saldo:"+r.getInt(1),r.getString(2));}
                try(var w=Files.newBufferedWriter(arquivo)){dados.store(w,"Fingerprints de registros anteriores; sem dados pessoais em claro");}System.out.println("OK: snapshot anterior guardado para conferência.");return;
            }
            try(var reader=Files.newBufferedReader(arquivo)){dados.load(reader);}
            for(String chave:dados.stringPropertyNames()){
                String[] partes=chave.split(":");if(partes[0].equals("saldo"))continue;
                try(PreparedStatement p=c.prepareStatement("SELECT md5(row(x.*)::text) FROM "+partes[0]+" x WHERE "+(partes[0].equals("pagamentos")?"id_pedido":"id")+"=?")){p.setLong(1,Long.parseLong(partes[1]));try(ResultSet r=p.executeQuery()){if(!r.next() || !r.getString(1).equals(dados.getProperty(chave)))throw new AssertionError("Registro anterior alterado: "+chave);}}
            }
            java.sql.Array chaves=c.createArrayOf("uuid",java.util.stream.IntStream.range(0,60).mapToObj(SimulacaoUsoDAO::chave).toArray(UUID[]::new));
            for(String chave:dados.stringPropertyNames())if(chave.startsWith("saldo:")){
                int id=Integer.parseInt(chave.substring(6));try(PreparedStatement p=c.prepareStatement("SELECT e.quantidade,COALESCE((SELECT SUM(i.quantidade) FROM itens_pedido i JOIN pedidos p ON p.id=i.id_pedido WHERE i.id_produto=e.id_produto AND p.chave_compra=ANY(?) AND p.status<>'cancelado'),0) FROM estoque e WHERE e.id_produto=?")){p.setArray(1,chaves);p.setInt(2,id);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)!=Integer.parseInt(dados.getProperty(chave))-r.getInt(2))throw new AssertionError("Saldo divergente para produto "+id);}}
            }
        }
        Sessao.iniciar(new UsuarioDAO().autenticar("admin","admin123"));var r=new SimulacaoUsoDAO().instalar(LocalDate.now());if(r.pedidosNovos()!=0 || r.pedidos()!=60 || r.confirmados()!=48 || r.cancelados()!=6 || r.pendentes()!=6)throw new AssertionError("Resumo/repetição incorretos.");
        TelaDashboard[] dashboard=new TelaDashboard[1];TelaGestaoPedidos[] pedidos=new TelaGestaoPedidos[1];
        SwingUtilities.invokeAndWait(()->{dashboard[0]=new TelaDashboard(null);preparar(dashboard[0]);pedidos[0]=new TelaGestaoPedidos(null);preparar(pedidos[0]);});CountDownLatch pronto=new CountDownLatch(1);SwingUtilities.invokeAndWait(()->{javax.swing.Timer timer=new javax.swing.Timer(100,null);timer.addActionListener(e->{if(texto(dashboard[0],"Período exibido") && !texto(pedidos[0],"Atualizando pedidos")){timer.stop();pronto.countDown();}});timer.start();});if(!pronto.await(35,TimeUnit.SECONDS))throw new AssertionError("Telas não carregaram.");
        SwingUtilities.invokeAndWait(()->{try{imagem(dashboard[0],"dashboard-uso-diario.png");imagem(pedidos[0],"pedidos-uso-diario.png");}catch(Exception e){throw new RuntimeException(e);}finally{for(Window w:Window.getWindows())w.dispose();}});
        var mes=new RelatorioDAO().consultar(LocalDate.now().withDayOfMonth(1),LocalDate.now());
        System.out.println("APROVADO: registros anteriores preservados, estoque conciliado por produto e carga repetida sem duplicação.");System.out.println("SIMULAÇÃO: "+r.pedidos()+" pedidos, "+r.confirmados()+" válidos confirmados, "+r.pendentes()+" pendentes, "+r.cancelados()+" cancelados; "+r.avaliacoes()+" avaliações; "+r.estoqueCritico()+" produtos críticos.");System.out.println("Faturamento fictício total: "+PagamentoSimulado.moeda(r.faturamento()));System.out.println("Dashboard no mês (inclui registros anteriores): "+PagamentoSimulado.moeda(mes.faturamento()));Sessao.encerrar();System.exit(0);
    }
    private static void preparar(JFrame f){f.addNotify();f.validate();}
    private static boolean texto(Container c,String trecho){for(Component f:c.getComponents()){if(f instanceof JLabel && ((JLabel)f).getText().contains(trecho))return true;if(f instanceof Container && texto((Container)f,trecho))return true;}return false;}
    private static void layout(Container c){c.doLayout();for(Component f:c.getComponents())if(f instanceof Container)layout((Container)f);}
    private static void imagem(JFrame f,String nome)throws Exception{Container p=f.getContentPane();layout(p);BufferedImage b=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();try{p.printAll(g);}finally{g.dispose();}ImageIO.write(b,"png",Path.of("docs/validacao-ui",nome).toFile());}
}
