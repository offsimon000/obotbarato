package view;

import model.Usuario;
import util.Sessao;
import util.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class TelaInicial extends JFrame {

    private Usuario usuarioLogado;

    public TelaInicial(Usuario usuario) {
        this.usuarioLogado = usuario;
        initComponents();
    }

    private void initComponents() {
        setTitle("OBotBarato");
        setSize(780, 540);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        JPanel painelPrincipal = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
            }
        };
        painelPrincipal.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1));

        MouseAdapter mover = new MouseAdapter() {
            Point inicio;
            public void mousePressed(MouseEvent e)  { inicio = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - inicio.x, p.y + e.getY() - inicio.y);
            }
        };

        // ── Cabecalho ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        header.addMouseListener(mover);
        header.addMouseMotionListener(mover);

        JPanel headerEsq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerEsq.setOpaque(false);
        JLabel lblNome = new JLabel("OBotBarato");
        lblNome.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblNome.setForeground(Tema.TEXTO_PRIMARIO);
        JLabel lblDot = new JLabel("*");
        lblDot.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblDot.setForeground(Tema.ROXO_PRIMARIO);
        headerEsq.add(lblDot);
        headerEsq.add(lblNome);
        header.add(headerEsq, BorderLayout.WEST);

        JPanel headerDir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        headerDir.setOpaque(false);
        JLabel lblUser = new JLabel(usuarioLogado.getNome());
        lblUser.setFont(Tema.FONTE_SUBTITULO);
        lblUser.setForeground(Tema.TEXTO_SECUNDARIO);
        JLabel lblPerfil = new JLabel(usuarioLogado.getPerfil().toUpperCase());
        lblPerfil.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblPerfil.setForeground(Tema.ROXO_PRIMARIO);
        lblPerfil.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Tema.ROXO_ESCURO, 1),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        JLabel lblFechar = new JLabel("  X");
        lblFechar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFechar.setForeground(Tema.TEXTO_HINT);
        lblFechar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblFechar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { lblFechar.setForeground(Tema.VERMELHO_ERRO); }
            public void mouseExited(MouseEvent e)  { lblFechar.setForeground(Tema.TEXTO_HINT); }
        });
        headerDir.add(lblUser);
        headerDir.add(lblPerfil);
        headerDir.add(Box.createHorizontalStrut(6));
        headerDir.add(lblFechar);
        header.add(headerDir, BorderLayout.EAST);
        painelPrincipal.add(header, BorderLayout.NORTH);

        // ── Grid de modulos ──
        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(28, 28, 16, 28));

        // Passa "this" para cada tela para que o botao Voltar funcione
        grid.add(criarCard("Produtos",    "Cadastro e categorias",  new Color(130, 80, 255),  () -> { setVisible(false); new TelaProdutos(this).setVisible(true); }));
        grid.add(criarCard("Clientes",    "Base de clientes",       new Color(60, 140, 255),  () -> { setVisible(false); new TelaClientes(this).setVisible(true); }));
        grid.add(criarCard("Pedidos",     "Vendas e carrinho",      new Color(255, 80, 120),  () -> { setVisible(false); new TelaPedidos(usuarioLogado, this).setVisible(true); }));
        grid.add(criarCard("Estoque",     "Entradas e saidas",      new Color(50, 200, 120),  () -> { setVisible(false); new TelaEstoque(this).setVisible(true); }));
        grid.add(criarCard("Relatorios",  "Vendas e analises",      new Color(255, 170, 40),  () -> { setVisible(false); new TelaRelatorios(this).setVisible(true); }));
        grid.add(criarCard("Sair",        "Voltar ao login",        new Color(100, 100, 120), () -> { Sessao.encerrar(); dispose(); new TelaLoginUnificado().setVisible(true); }));

        painelPrincipal.add(grid, BorderLayout.CENTER);

        // ── Rodape ──
        JLabel rodape = new JLabel("OBotBarato  2025 - Projeto Integrador", SwingConstants.CENTER);
        rodape.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rodape.setForeground(Tema.TEXTO_HINT);
        rodape.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        rodape.setOpaque(false);
        painelPrincipal.add(rodape, BorderLayout.SOUTH);

        setContentPane(painelPrincipal);
        setShape(new RoundRectangle2D.Double(0, 0, 780, 540, 16, 16));
    }

    private JPanel criarCard(String titulo, String sub, Color cor, Runnable acao) {
        JPanel card = new JPanel(new GridBagLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(cor);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), 4, 4, 4));
            }
        };
        card.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(16, 10, 6, 10);

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Tema.TEXTO_PRIMARIO);
        card.add(lblTitulo, gbc);

        JLabel lblSub = new JLabel(sub, SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = 1; gbc.insets = new Insets(0, 10, 16, 10);
        card.add(lblSub, gbc);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBorder(BorderFactory.createLineBorder(cor, 1)); card.repaint(); }
            public void mouseExited(MouseEvent e)  { card.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1)); card.repaint(); }
            public void mouseClicked(MouseEvent e) { acao.run(); }
        });
        return card;
    }
}