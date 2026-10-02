package view;
import util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Estrutura visual compartilhada pelos novos cadastros, usando o tema original. */
abstract class TelaManutencao extends JFrame {
    protected final DefaultTableModel modelo;
    protected final JTable tabela;
    protected final JPanel botoes=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
    TelaManutencao(JFrame anterior,String titulo,String[] colunas) {
        setTitle("OBotBarato - "+titulo);setSize(780,540);setLocationRelativeTo(anterior);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosing(java.awt.event.WindowEvent e){if(anterior!=null && Sessao.getIdUsuario()>0)anterior.setVisible(true);}});
        JPanel p=new JPanel(new BorderLayout(12,12));p.setBackground(Tema.BG_ESCURO);
        JPanel topo=new JPanel(new FlowLayout(FlowLayout.LEFT,16,12));topo.setBackground(Tema.BG_CARD);
        topo.add(Tema.botaoVoltar(this,anterior));JLabel nome=new JLabel(titulo);nome.setFont(Tema.FONTE_TITULO);topo.add(nome);p.add(topo,BorderLayout.NORTH);
        modelo=new DefaultTableModel(colunas,0){public boolean isCellEditable(int r,int c){return false;}};
        tabela=new JTable(modelo);Tema.aplicarTabelaDark(tabela);tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel centro=new JPanel(new BorderLayout());centro.setBackground(Tema.BG_ESCURO);centro.setBorder(BorderFactory.createEmptyBorder(0,14,0,14));centro.add(Tema.scrollDark(tabela));p.add(centro);
        botoes.setBackground(Tema.BG_CARD);p.add(botoes,BorderLayout.SOUTH);setContentPane(p);
    }
    protected void botao(String texto,Runnable acao){JButton b=Tema.botaoSecundario(texto);b.addActionListener(e->{try{acao.run();}catch(Exception ex){erro(ex);}});botoes.add(b);}
    protected int selecionado(){int r=tabela.getSelectedRow();if(r<0)throw new IllegalArgumentException("Selecione um cadastro.");return tabela.convertRowIndexToModel(r);}
    protected void erro(Throwable e){JOptionPane.showMessageDialog(this,Erros.mensagem(e),"Aviso",JOptionPane.WARNING_MESSAGE);}
    protected JPanel form(String[] nomes,JComponent[] campos){
        JPanel p=new JPanel(new GridLayout(0,1,4,5));p.setBackground(Tema.BG_ESCURO);
        for(int i=0;i<nomes.length;i++){JLabel l=new JLabel(nomes[i]);l.setFont(Tema.FONTE_LABEL);p.add(l);p.add(campos[i]);}return p;
    }
}
