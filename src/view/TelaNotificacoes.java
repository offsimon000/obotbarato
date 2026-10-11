package view;
import dao.NotificacaoDAO;
import util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Caixa de avisos da sessão, com atualização manual e leitura individual. */
public final class TelaNotificacoes extends JFrame {
    private final int usuario=Sessao.getIdUsuario(),cliente=Sessao.getIdCliente();
    private final DefaultTableModel modelo=new DefaultTableModel(new String[]{"Data","Tipo","Mensagem","Leitura"},0){public boolean isCellEditable(int r,int c){return false;}};
    private final JTable tabela=new JTable(modelo);
    private final JCheckBox naoLidas=new JCheckBox("Somente não lidas",true);
    private final JButton atualizar=Tema.botaoSecundario("Atualizar"),ler=Tema.botaoPrimario("Marcar como lida");
    private final JLabel aviso=new JLabel("Carregando notificações...");
    private final JTextArea detalhe=new JTextArea(3,40);
    private List<NotificacaoDAO.Aviso> itens=List.of();private long consulta;private boolean gravando;
    public TelaNotificacoes(JFrame anterior){
        if(usuario>0)Acesso.telaEquipe();else Acesso.telaCliente(cliente);
        setTitle("OBotBarato - Notificações");setSize(900,540);setLocationRelativeTo(anterior);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel p=new JPanel(new BorderLayout(12,12));p.setBackground(Tema.BG_ESCURO);p.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        JPanel topo=new JPanel(new BorderLayout());topo.setOpaque(false);JPanel esquerda=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));esquerda.setOpaque(false);esquerda.add(Tema.botaoVoltar(this,anterior,()->!gravando));JLabel titulo=new JLabel("Notificações");titulo.setFont(Tema.FONTE_TITULO);esquerda.add(titulo);topo.add(esquerda,BorderLayout.WEST);naoLidas.setOpaque(false);naoLidas.addActionListener(e->carregar());topo.add(naoLidas,BorderLayout.EAST);p.add(topo,BorderLayout.NORTH);
        Tema.aplicarTabelaDark(tabela);tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);tabela.getColumnModel().getColumn(0).setMinWidth(150);tabela.getColumnModel().getColumn(0).setPreferredWidth(170);tabela.getColumnModel().getColumn(1).setPreferredWidth(110);tabela.getColumnModel().getColumn(2).setPreferredWidth(500);tabela.getColumnModel().getColumn(3).setPreferredWidth(110);p.add(Tema.scrollDark(tabela));
        JPanel rodape=new JPanel(new BorderLayout(0,10));rodape.setOpaque(false);aviso.setFont(Tema.FONTE_LABEL);rodape.add(aviso,BorderLayout.NORTH);JPanel botoes=new JPanel(new FlowLayout(FlowLayout.RIGHT));botoes.setOpaque(false);atualizar.addActionListener(e->carregar());ler.addActionListener(e->marcar());botoes.add(atualizar);botoes.add(ler);rodape.add(botoes,BorderLayout.SOUTH);p.add(rodape,BorderLayout.SOUTH);
        detalhe.setEditable(false);detalhe.setLineWrap(true);detalhe.setWrapStyleWord(true);detalhe.setFont(Tema.FONTE_LABEL);rodape.add(Tema.scrollDark(detalhe),BorderLayout.CENTER);
        tabela.getSelectionModel().addListSelectionListener(e->{int linha=tabela.getSelectedRow();ler.setEnabled(!gravando && linha>=0);detalhe.setText(linha>=0 && linha<itens.size()?itens.get(tabela.convertRowIndexToModel(linha)).mensagem():"");});setContentPane(p);carregar();
    }
    private boolean mesmaSessao(){return usuario==Sessao.getIdUsuario() && cliente==Sessao.getIdCliente();}
    private void carregar(){if(gravando || !mesmaSessao())return;boolean filtro=naoLidas.isSelected();long versao=++consulta;ler.setEnabled(false);aviso.setText("Atualizando notificações...");
        new SwingWorker<List<NotificacaoDAO.Aviso>,Void>(){
            protected List<NotificacaoDAO.Aviso> doInBackground()throws Exception{return new NotificacaoDAO().listar(filtro);}
            protected void done(){if(!mesmaSessao() || consulta!=versao)return;try{itens=get();modelo.setRowCount(0);for(var n:itens)modelo.addRow(new Object[]{Datas.formatar(n.data()),n.tipo(),n.mensagem(),n.lida()?"Lida":"Não lida"});aviso.setText(itens.isEmpty()?"Nenhuma notificação neste filtro.":itens.size()+" avisos. Selecione um para marcar como lido.");}catch(Exception e){aviso.setText(Erros.mensagem(e));}}
        }.execute();
    }
    private void marcar(){int linha=tabela.getSelectedRow();if(gravando || linha<0 || !mesmaSessao())return;long id=itens.get(tabela.convertRowIndexToModel(linha)).id();gravando=true;++consulta;ler.setEnabled(false);atualizar.setEnabled(false);naoLidas.setEnabled(false);
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{new NotificacaoDAO().marcarLida(id);return null;}
            protected void done(){gravando=false;if(!mesmaSessao())return;atualizar.setEnabled(true);naoLidas.setEnabled(true);try{get();carregar();}catch(Exception e){aviso.setText(Erros.mensagem(e));}}
        }.execute();
    }
}
