package view;

import dao.UsuarioDAO;
import model.Usuario;
import util.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class TelaLogin extends JFrame {

    private JTextField txtLogin;
    private JPasswordField txtSenha;
    private JButton btnEntrar;
    private JLabel lblStatus;

    public TelaLogin() {
        initComponents();
    }

    private void initComponents() {
        setTitle("OBotBarato");
        setSize(440, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true); // remove barra padrão do sistema

        JPanel painel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            }
        };
        painel.setLayout(new GridBagLayout());
        painel.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1));

        // Arrastar janela
        MouseAdapter mover = new MouseAdapter() {
            Point inicio;
            public void mousePressed(MouseEvent e)  { inicio = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - inicio.x, p.y + e.getY() - inicio.y);
            }
        };
        painel.addMouseListener(mover);
        painel.addMouseMotionListener(mover);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 30, 8, 30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        // Ícone / logo texto
        JLabel lblIcon = new JLabel("", SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        lblIcon.setForeground(Tema.ROXO_PRIMARIO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(30, 30, 5, 30);
        painel.add(lblIcon, gbc);

        // Nome do sistema
        JLabel lblNome = new JLabel("OBotBarato", SwingConstants.CENTER);
        lblNome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblNome.setForeground(Tema.TEXTO_PRIMARIO);
        gbc.gridy = 1; gbc.insets = new Insets(0, 30, 4, 30);
        painel.add(lblNome, gbc);

        JLabel lblSub = new JLabel("Sistema de Gerenciamento", SwingConstants.CENTER);
        lblSub.setFont(Tema.FONTE_SUBTITULO);
        lblSub.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = 2; gbc.insets = new Insets(0, 30, 25, 30);
        painel.add(lblSub, gbc);

        // Label usuário
        JLabel lblLUser = new JLabel("USUÁRIO");
        lblLUser.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLUser.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = 3; gbc.insets = new Insets(5, 30, 3, 30);
        painel.add(lblLUser, gbc);

        txtLogin = Tema.campo(18);
        gbc.gridy = 4; gbc.insets = new Insets(0, 30, 10, 30);
        painel.add(txtLogin, gbc);

        // Label senha
        JLabel lblLSenha = new JLabel("SENHA");
        lblLSenha.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLSenha.setForeground(Tema.TEXTO_SECUNDARIO);
        gbc.gridy = 5; gbc.insets = new Insets(5, 30, 3, 30);
        painel.add(lblLSenha, gbc);

        txtSenha = Tema.campSenha(18);
        gbc.gridy = 6; gbc.insets = new Insets(0, 30, 18, 30);
        painel.add(txtSenha, gbc);

        // Botão entrar
        btnEntrar = Tema.botaoPrimario("ENTRAR");
        btnEntrar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnEntrar.setPreferredSize(new Dimension(0, 46));
        gbc.gridy = 7; gbc.insets = new Insets(0, 30, 10, 30);
        painel.add(btnEntrar, gbc);

        // Status
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(Tema.VERMELHO_ERRO);
        gbc.gridy = 8; gbc.insets = new Insets(0, 30, 10, 30);
        painel.add(lblStatus, gbc);

        // Fechar janela (X no canto)
        JLabel lblFechar = new JLabel("X", SwingConstants.RIGHT);
        lblFechar.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblFechar.setForeground(Tema.TEXTO_HINT);
        lblFechar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblFechar.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { lblFechar.setForeground(Tema.VERMELHO_ERRO); }
            public void mouseExited(MouseEvent e)  { lblFechar.setForeground(Tema.TEXTO_HINT); }
        });
        gbc.gridy = 9; gbc.insets = new Insets(0, 30, 15, 30);
        painel.add(lblFechar, gbc);

        setContentPane(painel);
        setShape(new RoundRectangle2D.Double(0, 0, 440, 520, 20, 20));

        btnEntrar.addActionListener(e -> fazerLogin());
        txtSenha.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) fazerLogin();
            }
        });
    }

    private void fazerLogin() {
        String login = txtLogin.getText().trim();
        String senha = new String(txtSenha.getPassword());
        if (login.isEmpty() || senha.isEmpty()) {
            lblStatus.setText("Preencha todos os campos.");
            return;
        }
        UsuarioDAO dao = new UsuarioDAO();
        Usuario usuario = dao.autenticar(login, senha);
        if (usuario != null) {
            dispose();
            new TelaInicial(usuario).setVisible(true);
        } else {
            lblStatus.setText("Usuário ou senha inválidos.");
            txtSenha.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}