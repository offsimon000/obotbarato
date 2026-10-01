package view.loja;

import dao.ClienteLojaDAO;
import model.Cliente;
import util.TemaLoja;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Tela de Login/Cadastro da loja (acesso direto pelo cliente).
 * Usada quando o cliente abre a loja diretamente, sem passar pelo login unificado.
 */
public class TelaLojaLogin extends JFrame {

    private final ClienteLojaDAO clienteDAO = new ClienteLojaDAO();

    public TelaLojaLogin() {
        initComponents();
    }

    private void initComponents() {
        setTitle("OBotBarato - Loja");
        setSize(440, 540);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setContentPane(criarPrincipal());
        setShape(new RoundRectangle2D.Double(0, 0, 440, 540, 20, 20));
    }

    private JPanel criarPrincipal() {
        JPanel p = new JPanel(new GridBagLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            }
        };
        p.setBorder(BorderFactory.createLineBorder(TemaLoja.BORDA, 1));

        MouseAdapter drag = criarDrag();
        p.addMouseListener(drag);
        p.addMouseMotionListener(drag);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 30, 6, 30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        gbc.gridy = 0; gbc.insets = new Insets(28, 30, 20, 30);
        p.add(criarLogo(), gbc);

        gbc.gridy = 1; gbc.insets = new Insets(0, 20, 0, 20);
        p.add(criarAbas(), gbc);

        JButton btnX = new JButton("Fechar");
        btnX.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnX.setForeground(TemaLoja.TEXTO_HINT);
        btnX.setContentAreaFilled(false);
        btnX.setBorderPainted(false);
        btnX.setFocusPainted(false);
        btnX.setHorizontalAlignment(SwingConstants.CENTER);
        btnX.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnX.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
            public void mouseEntered(MouseEvent e) { btnX.setForeground(TemaLoja.VERMELHO_ERRO); }
            public void mouseExited(MouseEvent e)  { btnX.setForeground(TemaLoja.TEXTO_HINT); }
        });
        gbc.gridy = 2; gbc.insets = new Insets(10, 30, 16, 30);
        p.add(btnX, gbc);
        return p;
    }

    private JPanel criarLogo() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        linha.setOpaque(false);
        JLabel estrela = new JLabel("*");
        estrela.setFont(TemaLoja.FONTE_TITULO_GRANDE);
        estrela.setForeground(TemaLoja.ROSA_PRIMARIO);
        JLabel nome = new JLabel("OBotBarato");
        nome.setFont(TemaLoja.FONTE_TITULO_GRANDE);
        nome.setForeground(TemaLoja.TEXTO_PRIMARIO);
        linha.add(estrela); linha.add(nome);
        JLabel sub = new JLabel("Sua loja de cosmeticos online", SwingConstants.CENTER);
        sub.setFont(TemaLoja.FONTE_LABEL);
        sub.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        p.add(linha, BorderLayout.NORTH);
        p.add(sub,   BorderLayout.SOUTH);
        return p;
    }

    private JTabbedPane criarAbas() {
        JTabbedPane abas = new JTabbedPane();
        abas.setBackground(TemaLoja.BG_CARD);
        abas.setForeground(TemaLoja.TEXTO_PRIMARIO);
        abas.setFont(new Font("Segoe UI", Font.BOLD, 13));
        abas.addTab("  Entrar  ",    criarPainelLogin());
        abas.addTab("  Cadastrar  ", criarPainelCadastro());
        return abas;
    }

    // ── Aba Login ──────────────────────────────────────────────────────
    private JPanel criarPainelLogin() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(TemaLoja.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JTextField     txtEmail = TemaLoja.campo(18);
        JPasswordField txtSenha = TemaLoja.campoSenha(18);
        JLabel         lblMsg   = criarLblStatus();

        GridBagConstraints gbc = gbc();
        gbc.gridy = 0; p.add(TemaLoja.rotulo("EMAIL"), gbc);
        gbc.gridy = 1; gbc.insets = new Insets(2,0,10,0); p.add(txtEmail, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(4,0,2,0);  p.add(TemaLoja.rotulo("SENHA"), gbc);
        gbc.gridy = 3; gbc.insets = new Insets(2,0,14,0); p.add(txtSenha, gbc);

        JButton btnEntrar = TemaLoja.botaoPrimario("Entrar na loja");
        btnEntrar.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 4; gbc.insets = new Insets(0,0,8,0); p.add(btnEntrar, gbc);
        gbc.gridy = 5;                                    p.add(lblMsg, gbc);

        btnEntrar.addActionListener(e -> fazerLogin(txtEmail, txtSenha, lblMsg, btnEntrar));
        txtSenha.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) btnEntrar.doClick();
            }
        });
        return p;
    }

    // ── Aba Cadastro ───────────────────────────────────────────────────
    private JPanel criarPainelCadastro() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(TemaLoja.BG_CARD);
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));

        JTextField     fNome     = TemaLoja.campo(18);
        JTextField     fCpf      = TemaLoja.campo(14);
        JTextField     fEmail    = TemaLoja.campo(18);
        JTextField     fTelefone = TemaLoja.campo(15);
        JTextField     fEndereco = TemaLoja.campo(20);
        JPasswordField fSenha    = TemaLoja.campoSenha(18);
        JPasswordField fConfir   = TemaLoja.campoSenha(18);
        JLabel         lblMsg    = criarLblStatus();

        String[] rotulos = {"NOME COMPLETO","CPF","EMAIL","TELEFONE","ENDERECO","SENHA","CONFIRMAR SENHA"};
        Component[] comps = {fNome, fCpf, fEmail, fTelefone, fEndereco, fSenha, fConfir};

        GridBagConstraints gbc = gbc();
        for (int i = 0; i < rotulos.length; i++) {
            gbc.gridy = i * 2;     gbc.insets = new Insets(3,0,1,0); p.add(TemaLoja.rotulo(rotulos[i]), gbc);
            gbc.gridy = i * 2 + 1; gbc.insets = new Insets(1,0,4,0); p.add(comps[i], gbc);
        }

        JButton btnCad = TemaLoja.botaoRoxo("Criar conta");
        btnCad.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 14; gbc.insets = new Insets(4,0,6,0); p.add(btnCad, gbc);
        gbc.gridy = 15; p.add(lblMsg, gbc);

        btnCad.addActionListener(e ->
            fazerCadastro(fNome, fCpf, fEmail, fTelefone, fEndereco, fSenha, fConfir, lblMsg));
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // LÓGICA
    // ═══════════════════════════════════════════════════════════════

    private void fazerLogin(JTextField txtEmail, JPasswordField txtSenha,
                             JLabel lblMsg, JButton btnEntrar) {
        String email = txtEmail.getText().trim();
        String senha = new String(txtSenha.getPassword());
        if (email.isEmpty() || senha.isEmpty()) {
            mostrarErro(lblMsg, "Preencha todos os campos."); return;
        }
        btnEntrar.setEnabled(false);
        lblMsg.setForeground(TemaLoja.TEXTO_SECUNDARIO);
        lblMsg.setText("Verificando...");

        SwingWorker<Cliente, Void> worker = new SwingWorker<>() {
            protected Cliente doInBackground() { return clienteDAO.autenticar(email, senha); }
            protected void done() {
                btnEntrar.setEnabled(true);
                try {
                    Cliente c = get();
                    if (c != null) { dispose(); new TelaLoja(c).setVisible(true); }
                    else { txtSenha.setText(""); mostrarErro(lblMsg, "Email ou senha invalidos."); }
                } catch (Exception ex) {
                    mostrarErro(lblMsg, "Erro de conexao. Verifique o MySQL.");
                }
            }
        };
        worker.execute();
    }

    private void fazerCadastro(JTextField fNome, JTextField fCpf, JTextField fEmail,
                                JTextField fTelefone, JTextField fEndereco,
                                JPasswordField fSenha, JPasswordField fConfir, JLabel lblMsg) {
        String senha  = new String(fSenha.getPassword());
        String confir = new String(fConfir.getPassword());

        if (!senha.equals(confir)) { mostrarErro(lblMsg, "As senhas nao conferem."); return; }

        // DAO valida e retorna mensagem de erro, ou null se ok
        String erro = clienteDAO.cadastrar(
            fNome.getText(), fCpf.getText(), fEmail.getText(),
            fTelefone.getText(), fEndereco.getText(), senha);

        if (erro == null) {
            lblMsg.setForeground(TemaLoja.VERDE_PRECO);
            lblMsg.setText("Conta criada! Faca login agora.");
            fNome.setText(""); fCpf.setText(""); fEmail.setText("");
            fTelefone.setText(""); fEndereco.setText("");
            fSenha.setText(""); fConfir.setText("");
        } else {
            mostrarErro(lblMsg, erro);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private JLabel criarLblStatus() {
        JLabel lbl = new JLabel("", SwingConstants.CENTER);
        lbl.setFont(TemaLoja.FONTE_LABEL);
        return lbl;
    }

    private void mostrarErro(JLabel lbl, String msg) {
        lbl.setForeground(TemaLoja.VERMELHO_ERRO);
        lbl.setText(msg);
    }

    private GridBagConstraints gbc() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 2;
        g.insets = new Insets(4, 0, 2, 0);
        return g;
    }

    private MouseAdapter criarDrag() {
        return new MouseAdapter() {
            private Point inicio;
            public void mousePressed(MouseEvent e)  { inicio = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - inicio.x, loc.y + e.getY() - inicio.y);
            }
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLojaLogin().setVisible(true));
    }
}