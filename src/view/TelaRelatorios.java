package view;

import dao.RelatorioDAO;
import util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Visão geral e relatórios compartilham período, regras financeiras e snapshot. */
public class TelaRelatorios extends JFrame {
    private final int usuario=Sessao.getIdUsuario();
    private final JTextField inicio=Tema.campo(10),fim=Tema.campo(10);
    private final JButton atualizar=Tema.botaoPrimario("Atualizar"),exportar=Tema.botaoSecundario("Exportar CSV");
    private final JLabel situacao=new JLabel("Carregando…"),regra=new JLabel();
    private final JTabbedPane abas=new JTabbedPane();
    private final JTable[] tabelas=new JTable[4];
    private final JLabel[] indicadores=new JLabel[6];
    private final Grafico vendas=new Grafico("Faturamento nos últimos 7 dias do período"),status=new Grafico("Pedidos por situação");
    private RelatorioDAO.Resultado resultado;
    private boolean carregando,exportando;

    public TelaRelatorios(JFrame anterior){this(anterior,false);}
    protected TelaRelatorios(JFrame anterior,boolean dashboard){
        Acesso.telaEquipe();setTitle("OBotBarato - "+(dashboard?"Dashboard":"Relatórios"));setSize(1060,720);setMinimumSize(new Dimension(960,640));setLocationRelativeTo(anterior);
        JPanel painel=new JPanel(new BorderLayout(12,12));painel.setBackground(Tema.BG_ESCURO);
        JPanel cabecalho=new JPanel(new FlowLayout(FlowLayout.LEFT,12,12));cabecalho.setBackground(Tema.BG_CARD);
        cabecalho.add(Tema.botaoVoltar(this,anterior,()->!exportando));JLabel titulo=new JLabel(dashboard?"Dashboard":"Relatórios");titulo.setFont(Tema.FONTE_TITULO);cabecalho.add(titulo);
        painel.add(cabecalho,BorderLayout.NORTH);
        JPanel centro=new JPanel(new BorderLayout(10,12));centro.setOpaque(false);centro.setBorder(BorderFactory.createEmptyBorder(0,18,0,18));
        JPanel filtros=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));filtros.setOpaque(false);
        MascaraCampo.aplicarData(inicio);MascaraCampo.aplicarData(fim);inicio.setName("relatorio-inicio");fim.setName("relatorio-fim");
        inicio.setText(Datas.formatar(LocalDate.now().withDayOfMonth(1)));fim.setText(Datas.formatar(LocalDate.now()));
        filtros.add(new JLabel("De:"));filtros.add(inicio);filtros.add(new JLabel("Até:"));filtros.add(fim);filtros.add(atualizar);filtros.add(exportar);
        centro.add(filtros,BorderLayout.NORTH);abas.setFont(Tema.FONTE_BOTAO);abas.addTab("Visão geral",visaoGeral());
        tabelas[0]=tabela("Data","Vendas confirmadas","Bruto (R$)","Cupom (R$)","Desc. pagamento (R$)","Líquido (R$)");
        tabelas[1]=tabela("ID","Produto","Unidades vendidas","Bruto dos itens (R$)");
        tabelas[2]=tabela("Pedido","Data","Cliente","Origem","Situação","Pagamento","Bruto (R$)","Cupom (R$)","Desc. pagamento (R$)","Total do pedido (R$)");
        tabelas[3]=tabela("ID","Produto","Saldo atual","Mínimo");
        for(int i=0;i<tabelas.length;i++)abas.addTab(new String[]{"Vendas por dia","Mais vendidos","Pedidos","Estoque baixo"}[i],Tema.scrollDark(tabelas[i]));
        tabelas[1].getColumnModel().getColumn(1).setPreferredWidth(400);tabelas[3].getColumnModel().getColumn(1).setPreferredWidth(400);
        tabelas[2].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);for(int i=0;i<10;i++)tabelas[2].getColumnModel().getColumn(i).setPreferredWidth(i==2?200:i==1?145:125);
        centro.add(abas);painel.add(centro);
        JPanel rodape=new JPanel(new GridLayout(2,1,0,7));rodape.setOpaque(false);rodape.setBorder(BorderFactory.createEmptyBorder(0,20,16,20));situacao.setFont(Tema.FONTE_SUBTITULO);regra.setFont(Tema.FONTE_SUBTITULO);regra.setForeground(Tema.TEXTO_SECUNDARIO);rodape.add(situacao);rodape.add(regra);painel.add(rodape,BorderLayout.SOUTH);setContentPane(painel);
        atualizar.addActionListener(e->carregar());inicio.addActionListener(e->carregar());fim.addActionListener(e->carregar());exportar.addActionListener(e->escolherExportacao());abas.addChangeListener(e->estado());
        DocumentListener edicao=new DocumentListener(){public void insertUpdate(DocumentEvent e){estado();}public void removeUpdate(DocumentEvent e){estado();}public void changedUpdate(DocumentEvent e){estado();}};
        inicio.getDocument().addDocumentListener(edicao);fim.getDocument().addDocumentListener(edicao);abas.setSelectedIndex(dashboard?0:1);carregar();
    }
    private JPanel visaoGeral(){
        JPanel p=new JPanel(new BorderLayout(12,16));p.setOpaque(false);p.setBorder(BorderFactory.createEmptyBorder(16,4,4,4));
        JPanel cards=new JPanel(new GridLayout(2,3,12,12));cards.setOpaque(false);
        String[] nomes={"Faturamento líquido","Vendas confirmadas","Ticket médio","Pagamento pendente","Pedidos cancelados","Estoque crítico · atual"};
        for(int i=0;i<6;i++){JPanel card=new JPanel(new BorderLayout(0,8));card.setBackground(Tema.BG_CARD);card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BORDA),BorderFactory.createEmptyBorder(14,16,14,16)));JLabel nome=new JLabel(nomes[i]);nome.setFont(Tema.FONTE_SUBTITULO);nome.setForeground(Tema.TEXTO_SECUNDARIO);indicadores[i]=new JLabel("—");indicadores[i].setFont(Tema.FONTE_TITULO);indicadores[i].setForeground(Tema.ROXO_ESCURO);card.add(nome,BorderLayout.NORTH);card.add(indicadores[i]);cards.add(card);}
        p.add(cards,BorderLayout.NORTH);JPanel graficos=new JPanel(new GridLayout(1,2,16,0));graficos.setOpaque(false);graficos.add(vendas);graficos.add(status);p.add(graficos);return p;
    }
    private JTable tabela(String... colunas){
        DefaultTableModel modelo=new DefaultTableModel(colunas,0){public boolean isCellEditable(int r,int c){return false;}};JTable t=new JTable(modelo);Tema.aplicarTabelaDark(t);
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable tabela,Object valor,boolean selecionada,boolean foco,int linha,int coluna){
            Object exibido=valor instanceof BigDecimal?PagamentoSimulado.moeda((BigDecimal)valor):valor instanceof LocalDate?Datas.formatar((LocalDate)valor):valor instanceof java.sql.Timestamp?Datas.formatar((java.sql.Timestamp)valor):valor;
            super.getTableCellRendererComponent(tabela,exibido,selecionada,foco,linha,coluna);setBorder(BorderFactory.createEmptyBorder(0,8,0,8));setHorizontalAlignment(valor instanceof Number?RIGHT:LEFT);return this;
        }});return t;
    }
    private void carregar(){
        if(carregando || exportando)return;LocalDate de,ate;
        try{de=Datas.ler(inicio.getText());ate=Datas.ler(fim.getText());if(ate.isBefore(de))throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");}
        catch(Exception e){situacao.setText(Erros.mensagem(e));situacao.setForeground(Tema.VERMELHO_ERRO);estado();return;}
        carregando=true;situacao.setText("Consultando dados…");situacao.setForeground(Tema.TEXTO_SECUNDARIO);estado();
        new SwingWorker<RelatorioDAO.Resultado,Void>(){
            protected RelatorioDAO.Resultado doInBackground()throws Exception{return new RelatorioDAO().consultar(de,ate);}
            protected void done(){carregando=false;if(Sessao.getIdUsuario()!=usuario || !isDisplayable())return;
                try{resultado=get();preencher();situacao.setForeground(Tema.TEXTO_PRIMARIO);situacao.setText("Período exibido: "+Datas.formatar(resultado.inicio())+" a "+Datas.formatar(resultado.fim())+" · "+resultado.pedidos().size()+" pedidos · Faturamento: "+PagamentoSimulado.moeda(resultado.faturamento()));}
                catch(Exception e){resultado=null;limpar();situacao.setText(Erros.mensagem(e));situacao.setForeground(Tema.VERMELHO_ERRO);}estado();
            }
        }.execute();
    }
    private void limpar(){for(JTable t:tabelas)((DefaultTableModel)t.getModel()).setRowCount(0);for(JLabel l:indicadores)l.setText("—");vendas.valores=List.of();status.valores=List.of();vendas.repaint();status.repaint();}
    private void preencher(){
        limpar();DefaultTableModel m=(DefaultTableModel)tabelas[0].getModel();for(var v:resultado.vendas())m.addRow(new Object[]{v.data(),v.pedidos(),v.bruto(),v.cupom(),v.pagamento(),v.liquido()});
        m=(DefaultTableModel)tabelas[1].getModel();for(var r:resultado.ranking())m.addRow(new Object[]{r.id(),r.nome(),r.quantidade(),r.bruto()});
        m=(DefaultTableModel)tabelas[2].getModel();for(var r:resultado.pedidos())m.addRow(new Object[]{r.id(),r.data(),r.cliente(),r.origem().equals("loja")?"Loja":"Balcão",StatusPedido.nome(r.status()),nomePagamento(r.pagamento()),r.bruto(),r.cupom(),r.descontoPagamento(),r.liquido()});
        m=(DefaultTableModel)tabelas[3].getModel();for(var r:resultado.estoque())m.addRow(new Object[]{r.id(),r.nome(),r.quantidade(),r.minimo()});
        String[] textos={PagamentoSimulado.moeda(resultado.faturamento()),Long.toString(resultado.confirmados()),PagamentoSimulado.moeda(resultado.ticket()),Long.toString(resultado.pendentes()),Long.toString(resultado.cancelados()),Integer.toString(resultado.estoque().size())};for(int i=0;i<6;i++)indicadores[i].setText(textos[i]);
        List<Barra> dias=new ArrayList<>();LocalDate primeiro=resultado.fim().minusDays(6);if(primeiro.isBefore(resultado.inicio()))primeiro=resultado.inicio();
        for(LocalDate d=primeiro;!d.isAfter(resultado.fim());d=d.plusDays(1)){LocalDate data=d;BigDecimal valor=resultado.vendas().stream().filter(v->v.data().equals(data)).map(RelatorioDAO.VendaDia::liquido).findFirst().orElse(BigDecimal.ZERO);dias.add(new Barra(Datas.formatar(d).substring(0,5),valor.doubleValue(),PagamentoSimulado.moeda(valor)));}
        vendas.valores=List.copyOf(dias);status.valores=StatusPedido.TODOS.stream().map(s->new Barra(StatusPedido.nome(s),resultado.quantidadeStatus(s),Long.toString(resultado.quantidadeStatus(s)))).toList();vendas.repaint();status.repaint();
    }
    private String nomePagamento(String status){return switch(status){case "confirmado"->"Confirmado";case "pendente"->"Pendente";case "cancelado"->"Cancelado";default->"Sem registro";};}
    private boolean filtroAtual(){return resultado!=null && inicio.getText().equals(Datas.formatar(resultado.inicio())) && fim.getText().equals(Datas.formatar(resultado.fim()));}
    private void estado(){
        int aba=abas.getSelectedIndex();exportar.setEnabled(!carregando && !exportando && filtroAtual() && aba>0 && tabelas[aba-1].getRowCount()>0);atualizar.setEnabled(!carregando && !exportando);inicio.setEditable(!carregando && !exportando);fim.setEditable(!carregando && !exportando);
        String texto=switch(aba){case 2->"Top 10 do período: unidades e valor bruto dos itens, antes dos descontos.";case 3->"Inclui pendentes e cancelados; seus totais históricos não representam faturamento.";case 4->"Saldo atual dos produtos ativos com quantidade menor ou igual ao mínimo; independe das datas.";default->"Faturamento por data de criação: pagamentos confirmados, excluindo pedidos cancelados.";};
        if(resultado!=null && !filtroAtual() && !carregando)texto="Filtro alterado. Clique em Atualizar para aplicar as datas e liberar a exportação.";
        else if(aba>0 && resultado!=null && tabelas[aba-1].getRowCount()==0)texto="Nenhum registro nesta aba. "+texto;regra.setText(texto);
    }
    private void escolherExportacao(){
        if(!exportar.isEnabled())return;int aba=abas.getSelectedIndex();JTable t=tabelas[aba-1];List<String> colunas=new ArrayList<>();List<Object[]> linhas=new ArrayList<>();for(int i=0;i<t.getColumnCount();i++)colunas.add(t.getColumnName(i));for(int r=0;r<t.getRowCount();r++){Object[] linha=new Object[t.getColumnCount()];for(int c=0;c<linha.length;c++)linha[c]=t.getValueAt(r,c);linhas.add(linha);}
        JFileChooser escolha=new JFileChooser();escolha.setDialogTitle("Exportar "+abas.getTitleAt(aba));String nome="obotbarato-"+new String[]{"vendas","ranking","pedidos","estoque-atual"}[aba-1]+"-"+resultado.inicio()+"-"+resultado.fim()+".csv";escolha.setSelectedFile(new java.io.File(nome));escolha.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Relatório CSV (*.csv)","csv"));if(escolha.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path destino=escolha.getSelectedFile().toPath();if(!destino.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))destino=Path.of(destino+".csv");if(Files.exists(destino) && JOptionPane.showConfirmDialog(this,"Substituir o arquivo existente?","Exportar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        Path arquivo=destino;exportando=true;estado();
        new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{if(Sessao.getIdUsuario()!=usuario)throw new IllegalStateException("Sessão encerrada.");Acesso.equipe();ExportarCsv.salvar(arquivo,colunas,linhas);return null;}protected void done(){exportando=false;if(!isDisplayable() || Sessao.getIdUsuario()!=usuario)return;try{get();JOptionPane.showMessageDialog(TelaRelatorios.this,"Relatório salvo em:\n"+arquivo.toAbsolutePath());}catch(Exception e){JOptionPane.showMessageDialog(TelaRelatorios.this,Erros.mensagem(e),"Exportação",JOptionPane.WARNING_MESSAGE);}estado();}}.execute();
    }
    private record Barra(String nome,double valor,String texto) {}
    private static final class Grafico extends JPanel {
        private final String titulo;private List<Barra> valores=List.of();
        Grafico(String titulo){this.titulo=titulo;setBackground(Tema.BG_CARD);setBorder(BorderFactory.createLineBorder(Tema.BORDA));setPreferredSize(new Dimension(400,300));}
        protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D p=(Graphics2D)g.create();try{p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);p.setFont(Tema.FONTE_BOTAO);p.setColor(Tema.TEXTO_PRIMARIO);p.drawString(titulo,16,26);if(valores.isEmpty()){p.setFont(Tema.FONTE_LABEL);p.drawString("Aguardando consulta",16,60);return;}double max=valores.stream().mapToDouble(Barra::valor).max().orElse(0);int passo=Math.max(24,Math.min(40,(getHeight()-70)/valores.size()));int largura=Math.max(40,getWidth()-225);p.setFont(Tema.FONTE_LABEL);for(int i=0;i<valores.size();i++){Barra b=valores.get(i);int y=53+i*passo;p.setColor(Tema.TEXTO_SECUNDARIO);p.drawString(b.nome(),16,y+15);p.setColor(Tema.BORDA);p.fillRoundRect(105,y,largura,20,6,6);p.setColor(Tema.ROXO_PRIMARIO);int w=max==0?0:(int)Math.round(largura*b.valor()/max);if(w>0)p.fillRoundRect(105,y,w,20,6,6);p.setColor(Tema.TEXTO_PRIMARIO);p.drawString(b.texto(),115+largura,y+15);}}finally{p.dispose();}}
    }
}
