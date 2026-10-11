package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/**
 * Tema visual da loja do cliente.
 * Paleta rosa/roxo vibrante para UX amigável.
 */
public class TemaLoja {

    // ── Backgrounds ──
    public static final Color BG_PRINCIPAL  = new Color(255, 255, 255);
    public static final Color BG_CARD       = new Color(248, 249, 250);
    public static final Color BG_INPUT      = new Color(255, 255, 255);
    public static final Color BORDA         = new Color(218, 220, 224);
    public static final Color BORDA_HOVER   = new Color(37, 99, 235);

    // ── Cores primarias ──
    public static final Color ROSA_PRIMARIO  = new Color(37, 99, 235);
    public static final Color ROSA_HOVER     = new Color(29, 78, 216);
    public static final Color ROXO_ACENTO    = new Color(59, 130, 246);
    public static final Color ROXO_HOVER     = new Color(37, 99, 235);

    // ── Cores semanticas ──
    public static final Color VERDE_PRECO    = new Color(22, 163, 74);
    public static final Color AMARELO_FAV    = new Color(217, 119, 6);
    public static final Color VERMELHO_ERRO  = new Color(220, 38, 38);
    public static final Color AZUL_INFO      = new Color(37, 99, 235);

    // ── Textos ──
    public static final Color TEXTO_PRIMARIO   = new Color(30, 41, 59);
    public static final Color TEXTO_SECUNDARIO = new Color(100, 116, 139);
    public static final Color TEXTO_HINT       = new Color(148, 163, 184);

    // ── Fontes ──
    public static final Font FONTE_TITULO_GRANDE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONTE_TITULO        = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONTE_SUBTITULO     = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONTE_PRODUTO       = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONTE_PRECO         = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONTE_BOTAO         = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONTE_CAMPO         = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONTE_LABEL         = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONTE_TOTAL         = new Font("Segoe UI", Font.BOLD, 18);

    // ═══════════════════════════════════════════════════════════════
    // COMPONENTES REUTILIZAVEIS
    // ═══════════════════════════════════════════════════════════════

    /** Botão primário — Ação principal, rosa */
    public static JButton botaoPrimario(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(ROSA_PRIMARIO);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONTE_BOTAO);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ROSA_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(ROSA_PRIMARIO); }
        });
        return btn;
    }

    /** Botão secundário — Roxo, ação alternativa */
    public static JButton botaoRoxo(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(ROXO_ACENTO);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONTE_BOTAO);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ROXO_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(ROXO_ACENTO); }
        });
        return btn;
    }

    /** Botão fantasia — Só contorno, sem preenchimento */
    public static JButton botaoFantasia(String texto) {
        JButton btn = new JButton(texto);
        btn.setBackground(BG_INPUT);
        btn.setForeground(TEXTO_PRIMARIO);
        btn.setFont(FONTE_BOTAO);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDA, 1),BorderFactory.createEmptyBorder(8,14,8,14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDA_HOVER, 1),BorderFactory.createEmptyBorder(8,14,8,14))); }
            public void mouseExited(MouseEvent e)  { btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDA, 1),BorderFactory.createEmptyBorder(8,14,8,14))); }
        });
        return btn;
    }

    /** Botão Voltar — Texto apenas, navegação */
    public static JButton botaoVoltar(JFrame telaAtual, JFrame telaAnterior) { return Tema.botaoVoltar(telaAtual,telaAnterior); }

    /** Campo de texto padrão da loja */
    public static JTextField campo(int colunas) {
        JTextField tf = new JTextField(colunas);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXTO_PRIMARIO);
        tf.setCaretColor(ROSA_PRIMARIO);
        tf.setFont(FONTE_CAMPO);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDA, 1),
            BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        return tf;
    }

    /** Campo de senha padrão da loja */
    public static JPasswordField campoSenha(int colunas) {
        JPasswordField pf = new JPasswordField(colunas);
        pf.setBackground(BG_INPUT);
        pf.setForeground(TEXTO_PRIMARIO);
        pf.setCaretColor(ROSA_PRIMARIO);
        pf.setFont(FONTE_CAMPO);
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDA, 1),
            BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        return pf;
    }

    /** ScrollPane com estilo da loja */
    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(BG_CARD);
        sp.getViewport().setBackground(BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(BORDA, 1));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Rótulo para campos — cinzento menor */
    public static JLabel rotulo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXTO_SECUNDARIO);
        return lbl;
    }

    /** Separador horizontal */
    public static JSeparator separador() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDA);
        sep.setBackground(BG_PRINCIPAL);
        return sep;
    }

    /** ComboBox padrão */
    public static JComboBox<String> combo(String[] itens) {
        JComboBox<String> cb = new JComboBox<>(itens);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXTO_PRIMARIO);
        cb.setFont(FONTE_CAMPO);
        cb.setBorder(BorderFactory.createLineBorder(BORDA, 1));
        return cb;
    }
}
