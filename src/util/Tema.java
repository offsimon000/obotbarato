package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class Tema {

    public static final Color BG_ESCURO        = new Color(255, 255, 255);
    public static final Color BG_CARD          = new Color(248, 249, 250);
    public static final Color BG_INPUT         = new Color(255, 255, 255);
    public static final Color BORDA            = new Color(218, 220, 224);
    public static final Color ROXO_PRIMARIO    = new Color(37, 99, 235);
    public static final Color ROXO_HOVER       = new Color(29, 78, 216);
    public static final Color ROXO_ESCURO      = new Color(30, 64, 175);
    public static final Color VERDE_SUCESSO    = new Color(22, 163, 74);
    public static final Color VERMELHO_ERRO    = new Color(220, 38, 38);
    public static final Color AMARELO_AVISO    = new Color(217, 119, 6);
    public static final Color TEXTO_PRIMARIO   = new Color(30, 41, 59);
    public static final Color TEXTO_SECUNDARIO = new Color(100, 116, 139);
    public static final Color TEXTO_HINT       = new Color(148, 163, 184);

    public static final Font FONTE_TITULO    = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONTE_SUBTITULO = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONTE_BOTAO     = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONTE_CAMPO     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONTE_TABELA    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONTE_LABEL     = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONTE_TOTAL     = new Font("Segoe UI", Font.BOLD, 18);

    public static void aplicarTabelaDark(JTable tabela) {
        tabela.setBackground(Color.WHITE);
        tabela.setForeground(TEXTO_PRIMARIO);
        tabela.setGridColor(BORDA);
        tabela.setFont(FONTE_TABELA);
        tabela.setRowHeight(30);
        tabela.setSelectionBackground(ROXO_PRIMARIO);
        tabela.setSelectionForeground(Color.WHITE);
        tabela.getTableHeader().setBackground(BG_CARD);
        tabela.getTableHeader().setForeground(TEXTO_SECUNDARIO);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.setShowHorizontalLines(true);
        tabela.setShowVerticalLines(false);
        tabela.setDefaultRenderer(Object.class,new javax.swing.table.DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object valor,boolean selecionado,boolean foco,int linha,int coluna){
                Component celula=super.getTableCellRendererComponent(t,valor,selecionado,foco,linha,coluna);
                setBorder(BorderFactory.createEmptyBorder(0,8,0,8));return celula;
            }
        });
    }

    public static JButton botaoPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(ROXO_PRIMARIO);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONTE_BOTAO);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ROXO_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(ROXO_PRIMARIO); }
        });
        return btn;
    }

    public static JButton botaoSecundario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(BG_INPUT);
        btn.setForeground(TEXTO_PRIMARIO);
        btn.setFont(FONTE_BOTAO);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDA, 1),BorderFactory.createEmptyBorder(8,14,8,14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(BORDA); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(BG_INPUT); }
        });
        return btn;
    }

    /** Botao Voltar — fecha a tela atual e reexibe o menu */
    public static JButton botaoVoltar(JFrame telaAtual, JFrame telaMenu) {
        JButton btn = new JButton("  < Voltar");
        btn.setForeground(TEXTO_SECUNDARIO);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 12));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(ROXO_PRIMARIO); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(TEXTO_SECUNDARIO); }
        });
        btn.addActionListener(e -> {
            telaAtual.dispose();
            if (telaMenu != null) telaMenu.setVisible(true);
        });
        return btn;
    }

    public static JTextField campo(int colunas) {
        JTextField tf = new JTextField(colunas);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXTO_PRIMARIO);
        tf.setCaretColor(ROXO_PRIMARIO);
        tf.setFont(FONTE_CAMPO);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDA, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    public static JPasswordField campSenha(int colunas) {
        JPasswordField pf = new JPasswordField(colunas);
        pf.setBackground(BG_INPUT);
        pf.setForeground(TEXTO_PRIMARIO);
        pf.setCaretColor(ROXO_PRIMARIO);
        pf.setFont(FONTE_CAMPO);
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDA, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return pf;
    }

    public static JScrollPane scrollDark(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_CARD);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(BORDA, 1));
        return sp;
    }
}
