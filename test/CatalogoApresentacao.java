import util.*;
import dao.*;
import java.sql.*;
import java.nio.file.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.concurrent.*;

/** Valida a substituição do catálogo só na base isolada, preservando pedidos e saldos. */
public final class CatalogoApresentacao {
    private static final CountDownLatch detalhes=new CountDownLatch(1);
    private static class LojaTeste extends view.loja.TelaLoja{
        LojaTeste(model.Cliente cliente){super(cliente);}
        protected int apresentarDetalhes(JPanel painel,Object[] opcoes){painel.addNotify();painel.setSize(painel.getPreferredSize());salvarImagem(painel,"catalogo-detalhes.png");detalhes.countDown();return -1;}
    }
    public static void main(String[] args)throws Exception{
        Thread vigia=new Thread(()->{try{Thread.sleep(120000);}catch(InterruptedException e){return;}System.err.println("Falha: catálogo excedeu limite.");System.exit(1);});vigia.setDaemon(true);vigia.start();
        exigir(texto("SELECT current_database()").equals("obotbarato_test"),"Base isolada obrigatória.");
        Sessao.iniciar(new UsuarioDAO().autenticar("admin","admin123"));
        model.Produto antigo=new model.Produto();antigo.setNome("Antigo Catalogo "+java.util.UUID.randomUUID());antigo.setPreco(new java.math.BigDecimal("12.34"));antigo.setAtivo(true);antigo.setIdCategoria(new CategoriaDAO().listar(false).get(0).id());antigo.setEstoqueInicial(9);new ProdutoDAO().salvar(antigo);Sessao.encerrar();
        long produtos=numero("SELECT COUNT(*) FROM produtos"),mov=numero("SELECT COUNT(*) FROM movimentos_estoque"),saldo=numero("SELECT SUM(quantidade) FROM estoque");
        String pedidos=texto("SELECT COALESCE(jsonb_agg(to_jsonb(p) ORDER BY p.id)::text,'[]') FROM pedidos p");
        sql("CREATE FUNCTION teste_catalogo_falha() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'falha proposital no arquivamento'; END $$");
        sql("CREATE TRIGGER teste_catalogo BEFORE UPDATE OF ativo ON produtos FOR EACH ROW WHEN (OLD.ativo AND NOT NEW.ativo) EXECUTE FUNCTION teste_catalogo_falha()");
        try{
            try{InstalarCatalogo.main(new String[]{"catalogo"});throw new AssertionError("Falha simulada foi ignorada.");}catch(SQLException esperado){}
            exigir(numero("SELECT COUNT(*) FROM produtos")==produtos && numero("SELECT COUNT(*) FROM movimentos_estoque")==mov && numero("SELECT SUM(quantidade) FROM estoque")==saldo,"Importação falha deixou dados parciais.");
        }finally{sql("DROP TRIGGER teste_catalogo ON produtos");sql("DROP FUNCTION teste_catalogo_falha()");Sessao.encerrar();}
        System.out.println("OK: falha após inserir os produtos desfaz catálogo, estoque e movimentos; antigos preservados.");
        InstalarCatalogo.main(new String[]{"catalogo"});
        exigir(numero("SELECT COUNT(*) FROM produtos WHERE ativo")==30,"Catálogo ativo não contém 30 produtos.");
        exigir(numero("SELECT COUNT(DISTINCT md5(imagem)) FROM produtos WHERE ativo")==30,"Imagens ausentes ou duplicadas.");
        exigir(numero("SELECT COUNT(*) FROM produtos WHERE ativo AND descricao IS NOT NULL AND preco>0 AND imagem IS NOT NULL")==30,"Cadastro incompleto.");
        exigir(numero("SELECT COUNT(*) FROM (SELECT id_categoria,COUNT(*) n FROM produtos WHERE ativo GROUP BY id_categoria HAVING COUNT(*)=6) c")==5,"Distribuição de categorias incorreta.");
        exigir(pedidos.equals(texto("SELECT COALESCE(jsonb_agg(to_jsonb(p) ORDER BY p.id)::text,'[]') FROM pedidos p")),"Importação alterou pedidos.");
        long totalProdutos=numero("SELECT COUNT(*) FROM produtos"),movimentos=numero("SELECT COUNT(*) FROM movimentos_estoque");
        int id=(int)numero("SELECT MIN(id) FROM produtos WHERE ativo");sql("UPDATE estoque SET quantidade=quantidade-1 WHERE id_produto="+id);
        long estoque=numero("SELECT quantidade FROM estoque WHERE id_produto="+id);
        InstalarCatalogo.main(new String[]{"catalogo"});
        exigir(numero("SELECT COUNT(*) FROM produtos")==totalProdutos && numero("SELECT COUNT(*) FROM movimentos_estoque")==movimentos && numero("SELECT quantidade FROM estoque WHERE id_produto="+id)==estoque,"Reexecução duplica ou repõe estoque.");
        System.out.println("OK: 30 produtos, 5 categorias, preços/descrições e 30 imagens únicas; reexecução não duplica nem repõe saldo.");
        model.Cliente cliente=new ClienteLojaDAO().autenticar("cliente@demo.local","cliente123");Sessao.iniciarCliente(cliente);
        final view.loja.TelaLoja[] loja=new view.loja.TelaLoja[1];
        SwingUtilities.invokeAndWait(()->{loja[0]=new LojaTeste(cliente);loja[0].addNotify();loja[0].validate();});
        CountDownLatch pronto=new CountDownLatch(1);
        SwingUtilities.invokeAndWait(()->{Timer timer=new Timer(100,null);timer.addActionListener(e->{if(temTexto(loja[0],"30 produtos encontrados")){timer.stop();pronto.countDown();}});timer.start();});
        exigir(pronto.await(30,TimeUnit.SECONDS),"Loja não carregou o catálogo.");
        SwingUtilities.invokeAndWait(()->botao(loja[0],"Ver detalhes").doClick());
        exigir(detalhes.await(30,TimeUnit.SECONDS),"Detalhes do produto não carregaram.");
        var catalogoVisual=new ProdutoDAO().listarCatalogo("");
        SwingUtilities.invokeAndWait(()->{try{
            Container p=loja[0].getContentPane();layout(p);BufferedImage imagem=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=imagem.createGraphics();try{p.printAll(g);}finally{g.dispose();}ImageIO.write(imagem,"png",Path.of("docs/validacao-ui/catalogo-apresentacao.png").toFile());
            JPanel grade=new JPanel(new GridLayout(5,6,12,12));grade.setBackground(Color.WHITE);grade.setSize(1320,1200);
            for(var disponivel:catalogoVisual){var produto=disponivel.produto();JPanel card=new JPanel(new BorderLayout());card.setBackground(Color.WHITE);card.add(Imagens.painel(produto.getImagem(),produto.getNome(),170));JTextArea nome=new JTextArea(produto.getNome()+"\n"+PagamentoSimulado.moeda(produto.getPrecoDecimal()),3,14);nome.setLineWrap(true);nome.setWrapStyleWord(true);nome.setEditable(false);nome.setFont(new Font("Segoe UI",Font.PLAIN,12));card.add(nome,BorderLayout.SOUTH);grade.add(card);}JFrame previa=new JFrame();previa.setContentPane(grade);previa.setSize(1320,1238);previa.addNotify();previa.validate();layout(grade);
            BufferedImage resumo=new BufferedImage(grade.getWidth(),grade.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D gg=resumo.createGraphics();try{grade.printAll(gg);}finally{gg.dispose();}ImageIO.write(resumo,"png",Path.of("docs/validacao-ui/catalogo-30-produtos.png").toFile());
        }catch(Exception e){throw new RuntimeException(e);}finally{for(Window w:Window.getWindows())w.dispose();Sessao.encerrar();}});
        System.out.println("APROVADO: importação atômica, históricos preservados, saldo idempotente e catálogo de 30 produtos carregado na loja.");System.exit(0);
    }
    private static JButton botao(Container c,String texto){for(Component f:c.getComponents()){if(f instanceof JButton && texto.equals(((JButton)f).getText()))return (JButton)f;if(f instanceof Container){JButton b=botao((Container)f,texto);if(b!=null)return b;}}return null;}
    private static void salvarImagem(Container painel,String arquivo){try{layout(painel);BufferedImage imagem=new BufferedImage(painel.getWidth(),painel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=imagem.createGraphics();try{painel.printAll(g);}finally{g.dispose();}ImageIO.write(imagem,"png",Path.of("docs/validacao-ui",arquivo).toFile());}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean temTexto(Container c,String prefixo){for(Component f:c.getComponents()){if(f instanceof JLabel && ((JLabel)f).getText().startsWith(prefixo))return true;if(f instanceof Container && temTexto((Container)f,prefixo))return true;}return false;}
    private static void layout(Container c){c.doLayout();for(Component f:c.getComponents())if(f instanceof Container)layout((Container)f);}
    private static String texto(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement();ResultSet r=p.executeQuery(s)){r.next();return r.getString(1);}}
    private static long numero(String s)throws SQLException{return Long.parseLong(texto(s));}
    private static void sql(String s)throws SQLException{try(Connection c=ConexaoBanco.getConexao();Statement p=c.createStatement()){p.execute(s);}}
    private static void exigir(boolean b,String s){if(!b)throw new AssertionError(s);}
}
